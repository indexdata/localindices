/* WebHarvestJob
 * Crawls around web sites and stores full text etc
 */
package com.indexdata.masterkey.localindices.harvest.job;

import com.indexdata.masterkey.localindices.entity.WebCrawlResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
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
 *   - filter what links to follow
 *   - Redirects (watch out for loops etc) (doesn't the library handle this?)
 *   - Get text into zebra
 *   - error handling, 
 *   - Parameters
 *     - Remove the filetype mask
 *     - Add max pages to harvest
 * TODO - tests
 *   - Test a site with plain text files
 *   - Test with a tarpit-like site
 * TODO - but in some later version
 *   - robots.txt
 *     - check user-agent lines
 *     - understand also Allow lines
 *   - better load reduction for servers.
 *     - keep todo list in a priority queue, with time stamps when a hosts turn is
 */
public class WebHarvestJob implements HarvestJob {

    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
    private HarvestStorage storage;
    private HarvestStatus status;
    private String error;
    private WebCrawlResource resource;
    private boolean die = false;
    private Vector<URL> toSearch; // todo list for this round
    private Vector<URL> searched; // all pages we have seen
    private Vector<URL> nextRound; // links found in this round are pushed here
    private int numtosearch; 
    private int round;
    private Map<URL,String> robotCache = new HashMap<URL,String>();
    
    private final static int connTimeOut = 3000; // ms to make a connection
    private final static int readTimeOut = 3000; // ms to read a block
      // About 30 seconds seems reasonable. 
    private final static int readBlockSize = 1000000; // bytes to read in one op
    private final static int maxReadSize =  10000000; // 10MB 
    private final static String userAgentString = "IndexData Masterkey Web crawler";

    private class pageInfo {

        public URL url;
        public String content; // the whole thing
        public String headers;
        public String body;
        public Vector<URL> links;
        public String plaintext;
        public String title;
    }

    public WebHarvestJob(WebCrawlResource resource) {
        this.resource = resource;
        this.status = HarvestStatus.NEW;
        logger.setLevel(Level.ALL);  // While debugging
        this.error=null;    
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

    private String fetchPage(URL url) {
        // FIXME - Return a pageinfo structure with all the data and error messages
        String pageUrl = url.toString();
        logger.log(Level.TRACE, "About to fetch '" + pageUrl + "'");
        if (url.getProtocol().compareTo("http") != 0) {
            logger.log(Level.ERROR, "Unsupported protocol in '" + pageUrl);
            return "";
        }
        try {
            URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(connTimeOut);
            urlConnection.setReadTimeout(readTimeOut);
            urlConnection.setAllowUserInteraction(false);
            urlConnection.setRequestProperty("User-agent",userAgentString );
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
            if (!type.startsWith("text/html") &&
                !type.startsWith("text/plain")  ) {
                // Get also plain text, we need it for robots.txt, and
                // might as well index it all anyway
                logger.log(Level.DEBUG, pageUrl + " skipped: type '" +
                        type + "' not text/html (nor text/plain)");
                return "";
            }
            // search the input stream for links
            // first, read in the entire URL
            byte b[] = new byte[readBlockSize];
            int numRead = urlStream.read(b);
            String content = new String(b, 0, numRead);
            //FIXME - Check if content all too big, abort! Could be a tar pit
            // or misbehaving web server that just keeps outputting rubbish
            while ((numRead != -1) && ( content.length() < maxReadSize) ) {
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
        } catch ( FileNotFoundException ex ) {
            logger.log(Level.ERROR, "I/O Exception: Not found " + pageUrl );
        } catch (IOException ex) {
            logger.log(Level.ERROR, "I/O Exception " +
                    "(" + ex.getClass().getSimpleName() +") "+
                    "with " + pageUrl + ": " + ex.getMessage());
        }
        return ""; // signals an error, real return is inside the try block
    } // fetchPage

    private pageInfo splitPage(String page, URL pageUrl) {
        pageInfo pi = new pageInfo();
        pi.links = new Vector<URL>();
        pi.content = page; // save it all for future reference
        pi.url = pageUrl;
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
            //logger.log(Level.TRACE, "Found link '" + m3.group() + "' '" + lnk + "' " +
            //        "-> '" + linkUrl.toString() + "'");
            if (!pi.links.contains(linkUrl)) {
                pi.links.add(linkUrl);
            }
        }

        logger.log(Level.DEBUG, 
                "JOB#" + resource.getId() + " " + 
                round +":" +searched.size() +"/" + numtosearch + " " + 
                pi.url + " " +
                "title:'" + pi.title + "' (" +
                "h=" + pi.headers.length() + "b " +
                "b=" + pi.body.length() + "b) " +
                pi.links.size() + " links");
        return pi;
    }

    private boolean checkRobots(URL url) {
        String strHost = url.getHost();
        if (strHost==null || strHost.isEmpty() ) {
            // Should not happen!
            logger.log(Level.DEBUG, "Could not extract host from '" +
                    url.toString()+"' - skipping robots txt");
	    return false;            
        }
	String strRobot = "http://" + strHost + "/robots.txt";
	URL robUrl;
	try { 
	    robUrl = new URL(strRobot);
	} catch (MalformedURLException e) {
	    // something weird is happening, so don't trust it
            logger.log(Level.DEBUG, "Could not create robot url "+
                    "'" + strRobot +"'");            
	    return false;
	}
        String robtxt=robotCache.get(robUrl);
        if ( robtxt == null ) {
            robtxt = fetchPage(robUrl);
            if (robtxt==null)
                robtxt="";
            robotCache.put(robUrl, robtxt); 
            logger.log(Level.DEBUG, "Got " + robUrl.toString()+ "\n" +
                    robtxt);
        }
        if (robtxt.isEmpty())
            return true; // no robots.txt, go ahead
        
        // Simplified, we assume all User-agent lines apply to us
        // Most likely nobody has (yet?) written a robots.txt section 
        // for specifically for us.
        Pattern p = Pattern.compile("^Disallow:\\s*(.*\\S)\\s*$",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE );
        Matcher m = p.matcher(robtxt);
        String urlpath=url.getPath();
        while (m.find()) {
            String path = m.group(1);
            if (urlpath.startsWith(path) ) {
                logger.log(Level.TRACE, "Path '"+urlpath+"' forbidden "+
                        "by robot '" + path +"'");                
                return false; // found one they don't want us to go to
            }
        }
        //logger.log(Level.TRACE, "Path '"+urlpath+"' all right ");
        return true;
    }
    
    private boolean filterLink(URL url) {
        if ( resource.getUriMasks().isEmpty() ) {
            return true; // no filtering, go anywhere
        }
        String urlStr = url.toString();
        for (String mask : resource.getUriMasks().split(" ") ) {
            Pattern p = Pattern.compile(mask);
            Matcher m = p.matcher(urlStr);
            if ( m.find() ) {
                logger.log(Level.TRACE, "url '"+urlStr+"' matched '" + mask + "'");
                return true;
            }
        }
        logger.log(Level.TRACE, "url '"+urlStr+"' refused by filters");
        return false;
    }
    
    private void initWorkList() {
        searched = new Vector<URL>();
        toSearch = new Vector<URL>();
        nextRound = new Vector<URL>();
        for (String startUrl : resource.getStartUrls().split(" ")) {
            URL url;
            try {
                url = new URL(startUrl);
                toSearch.add(url);
                logger.log(Level.TRACE, "Added start URL '" + startUrl + "'");
            } catch (MalformedURLException ex) {
                setError("Invalid start url: '" + startUrl+"'" );
                toSearch.clear();
            }
        // Fixme - validate they all are OK
        }
        numtosearch = toSearch.size();
    }

    /** Harvest one round of links */
    private void harvestRound() {
        while (!toSearch.isEmpty() && !isKillSendt()) {
            URL curUrl = toSearch.firstElement();
            toSearch.removeElementAt(0);
            if (!searched.contains(curUrl)) {
                searched.add(curUrl); // to make sure we don't go there again  
                if ( !checkRobots(curUrl))
                    break;
                //resource.setCurrentStatus("" + searched.size() + " pages visited");
                String page = fetchPage(curUrl);
                if (!page.isEmpty()) {
                    pageInfo pi = splitPage(page, curUrl);
                    for (URL u : pi.links) {
                        if (!nextRound.contains(u) && filterLink(u)) {
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
        long startTime = System.currentTimeMillis();
        initWorkList();
        while (round <= resource.getRecursionDepth() 
                && !toSearch.isEmpty() 
                && !isKillSendt()) {
            harvestRound();
            logger.log(Level.DEBUG, "Round " + round + ": " +
                    searched.size() + " urls seen. " +
                    nextRound.size() + " urls to go ");
            toSearch.addAll(nextRound);
            numtosearch += toSearch.size();
            nextRound.clear();
            round++;
        }
        long elapsed = (System.currentTimeMillis() - startTime) / 1000; // sec
        
        String killmsg="Did";
        if (isKillSendt()) 
            killmsg="Killed after";
        logger.log(Level.DEBUG, killmsg +" " + (round - 1) + " rounds. " +
                "Seen " + searched.size() + " urls " +
                " in " + elapsed + " seconds " +
                "(Next depth would have taken " + toSearch.size() + " pages more)");
    }

    public void run() {
        status = HarvestStatus.RUNNING;
        harvestLoop();
        if (this.error == null) {
            if (isKillSendt()) {
                setError("Web Crawl interrupted with a kill signal");
            } else {
                //status = HarvestStatus.FINISHED;
                setError("All done - but we call it an error so we can do again");
            }
        }
    } // run()
}

