/* WebHarvestJob
 * Crawls around web sites and stores full text etc
 */
package com.indexdata.masterkey.localindices.harvest.job;

import com.indexdata.masterkey.localindices.entity.WebCrawlResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/** WebHarvestJob
 * Crawls around web sites and stores full text, title, url, etc.
 *
 * @author heikki
 * 
 * TODO:
 *   - filter what types of links to follow
 *   - Robots.txt (with cache)
 *   - Redirects (watch out for loops etc)
 *   - better load reduction for servers.
 *   - Get text into zebra
 *   - error handling, time outs, etc
 */
public class WebHarvestJob implements HarvestJob {

    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
    private HarvestStorage storage;
    private HarvestStatus status;
    private String error;
    private WebCrawlResource resource;
    private boolean die = false;
    private Vector<String> toSearch; // todo list for this round
    private Vector<String> searched; // all pages we have seen
    private Vector<String> nextRound; // links found in this round are pushed here
    private int numtosearch; 
    private int round;

    private class pageInfo {

        public String url;
        public String content; // the whole thing
        public String headers;
        public String body;
        public Vector<String> links;
        public String plaintext;
        public String title;
    }

    public WebHarvestJob(WebCrawlResource resource) {
        this.resource = resource;
        this.status = HarvestStatus.NEW;
        logger.setLevel(Level.ALL);  // While debugging
    }

    private synchronized boolean isKillSendt() {
        if (die) {
            logger.log(Level.WARN, "Web harvest received kill signal.");
        }
        return die;
    }

    private synchronized void onKillSendt() {
        die = true;
    }

    public void kill() {
        if (status != HarvestStatus.FINISHED) {
            status = HarvestStatus.KILLED;
            onKillSendt();
        }
    }

    public HarvestStatus getStatus() {
        return status;
    }

    public void setStorage(HarvestStorage storage) {
        this.storage = storage;
    }

    public HarvestStorage getStorage() {
        return storage;
    }

    public void finishReceived() {
        status = HarvestStatus.WAITING;
    }

    private void setError(String e) {
        this.error = e;
        status = HarvestStatus.ERROR;
        resource.setError(e);
        logger.log(Level.ERROR, e);
    }

    public String getError() {
        return error;
    }

    private String fetchPage(String pageUrl) {
        // FIXME - Return a pageinfo structure with all the data and error messages
        // FIXME - Take the URL as an URL, not as a string
        logger.log(Level.TRACE, "About to fetch '" + pageUrl + "'");
        URL url;
        try {
            url = new URL(pageUrl);
        } catch (MalformedURLException e) {
            logger.log(Level.ERROR, "Invalid URL: '" + pageUrl + "': " + e.getMessage());
            return "";
        }
        if (url.getProtocol().compareTo("http") != 0) {
            logger.log(Level.ERROR, "Unsupported protocol in '" + pageUrl);
            return "";
        }
        try {
            URLConnection urlConnection = url.openConnection();
            urlConnection.setAllowUserInteraction(false);
            InputStream urlStream = url.openStream();
            String type = "";
            type = urlConnection.getContentType();
            if (type.isEmpty()) {
                type = URLConnection.guessContentTypeFromStream(urlStream);
            }
            if (type == null) {
                logger.log(Level.DEBUG, pageUrl + " skipped: could not guess type");
                return "";
            }
            if (!type.startsWith("text/html")) {
                logger.log(Level.DEBUG, pageUrl + " skipped: type '" +
                        type + "' not text/html");
                return "";
            }
            // search the input stream for links
            // first, read in the entire URL
            byte b[] = new byte[1000];
            int numRead = urlStream.read(b);
            String content = new String(b, 0, numRead);
            //FIXME - Check if content all too big, abort! Could be a tar pit
            // or misbehaving web server that just keeps outputting rubbish
            while (numRead != -1) {
                numRead = urlStream.read(b);
                if (numRead != -1) {
                    String newContent = new String(b, 0, numRead);
                    content += newContent;
                }
            }
            urlStream.close();
            logger.log(Level.TRACE, pageUrl + " Read " + content.length() + " bytes");
            //logger.log(Level.DEBUG, content );
            return content;
        } catch (IOException ex) {
            logger.log(Level.ERROR, "I/O Exception with " + pageUrl + ": " +
                    ex.getMessage());
            return "";
        }
    } // fetchPage

    private pageInfo splitPage(String page, String url) {
        pageInfo pi = new pageInfo();
        pi.links = new Vector<String>();
        pi.content = page; // save it all for future reference
        pi.url = url;
        // Split headers and body, if possible
        Pattern p1 = Pattern.compile("<head>(.*)</head>.*" +
                "<body[^>]*>(.*)",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m1 = p1.matcher(page);
        if (m1.find()) {
            //logger.log(Level.TRACE, "P1 matched.");
            pi.headers = m1.group(1);
            pi.body = m1.group(2);
        } else {
            logger.log(Level.TRACE, "P1 did NOT match. p='" + p1.pattern() + "'");
            pi.headers = "";
            pi.body = page; // doesn't look like good html, try to extract links anyway
        }
        // Extract a title
        pi.title = "";
        Pattern p2 = Pattern.compile("<title>\\s*(.*\\S)\\s*</title>",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m2 = p2.matcher(pi.headers);
        if (m2.find()) {
            pi.title = m2.group(1);
        } else {
            pi.title = "???"; // FIXME - try to get the first H1 tag, 
        // or first text line or something
        }
        // extract full text

        // extract links
        URL pageUrl;
        try {
            pageUrl = new URL(url);
        } catch (MalformedURLException ex) {
            return pi; // without extracting links, they won't be good without this
        }

        //Pattern p3 = Pattern.compile("<a([^>]+)>",
        Pattern p3 = Pattern.compile("<a[^>]+href=['\"]?([^>'\"#]+)['\"# ]?[^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m3 = p3.matcher(pi.body);
        while (m3.find()) {
            String lnk = m3.group(1);

            URL linkUrl;
            try {
                linkUrl = new URL(pageUrl, lnk);
            } catch (MalformedURLException ex) {
                logger.log(Level.TRACE, "Could not make a good url from '" + lnk + "'");
                break;
            }
            String newlnk = linkUrl.toString();
            //logger.log(Level.TRACE, "Found link '" + m3.group() + "' '" + lnk + "' " +
            //        "-> '" + newlnk + "'");
            if (!pi.links.contains(newlnk)) {
                pi.links.add(newlnk);
            }
        }

        logger.log(Level.DEBUG, 
                resource.getName() +
                " " + round +":" +searched.size() +"/" + numtosearch +
                " " + pi.url + " " +
                "title:'" + pi.title + "' (" +
                "h=" + pi.headers.length() + "b " +
                "b=" + pi.body.length() + "b) " +
                pi.links.size() + " links");


        return pi;
    }

    private void initWorkList() {
        searched = new Vector<String>();
        toSearch = new Vector<String>();
        nextRound = new Vector<String>();
        for (String startUrl : resource.getStartUrls().split(" ")) {
            toSearch.add(startUrl);
            logger.log(Level.TRACE, "Added start URL '" + startUrl + "'");
        // Fixme - validate they all are OK
        }
        numtosearch = toSearch.size();
    }

    /** Harvest one round of links */
    private void harvestRound() {
        while (!toSearch.isEmpty() && !isKillSendt()) {
            String curUrl = toSearch.firstElement();
            toSearch.removeElementAt(0);
            if (!searched.contains(curUrl)) {
                searched.add(curUrl); // to make sure we don't go there again  
                // Fixme - check robot safe
                resource.setCurrentStatus("" + searched.size() + "pages visited");
                String page = fetchPage(curUrl);
                if (!page.isEmpty()) {
                    pageInfo pi = splitPage(page, curUrl);
                    for (String u : pi.links) {
                        if (!nextRound.contains(u)) {
                            // FIXME - filter those links that need to be followed
                            nextRound.add(u);
                        }
                        try {
                            //Thread.sleep(1000); // simple load reduction on the servers
                            Thread.sleep(100); 
                        } catch (InterruptedException ex) {
                            break; // never mind if that sleep got interrupted
                        }
                    }
                }
            }
        }
    } // harvestRound

    /** Harvest all there is to do */
    private void harvestLoop() {
        round = 0;
        initWorkList();
        while (round <= resource.getRecursionDepth() && !isKillSendt()) {
            harvestRound();
            logger.log(Level.DEBUG, "Round " + round + ": " +
                    searched.size() + " urls seen. " +
                    nextRound.size() + " urls to go ");
            toSearch.addAll(nextRound);
            numtosearch += toSearch.size();
            nextRound.clear();
            round++;
        }
        logger.log(Level.DEBUG, "Did " + (round - 1) + " rounds. " +
                "(max=" + resource.getRecursionDepth() + ") " +
                "(iskilled=" + isKillSendt() + ") " +
                "Seen " + searched.size() + " urls");
    }

    public void run() {
        status = HarvestStatus.RUNNING;
        harvestLoop();
        if (isKillSendt()) {
            setError("Web Crawl interrupted with a kill signal");
        } else {
            //status = HarvestStatus.FINISHED;
            setError("All done - but we call it an error so we can do again");
        }
    } // run()
}

