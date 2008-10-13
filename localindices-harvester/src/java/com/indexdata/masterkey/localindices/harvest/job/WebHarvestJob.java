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
 */
public class WebHarvestJob implements HarvestJob {

    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
    private HarvestStorage storage;
    private HarvestStatus status;
    private String error;
    private WebCrawlResource resource;
    private boolean die = false;
    private Vector<String> toSearch;
    private Vector<String> searched;

    private class pageInfo {
        public String url;
        public String content; // the whole thing
        public String headers;
        public String body;
        public String[] links;
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
            logger.log(Level.WARN, "Bulk harvest received kill signal.");
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

    private void initWorkList() {
        searched = new Vector<String>();
        toSearch = new Vector<String>();
        for (String startUrl : resource.getStartUrls().split(" ")) {
            toSearch.add(startUrl);
            logger.log(Level.TRACE, "Added start URL '" + startUrl + "'");
        // Fixme - validate they all are OK
        }

    }

    private String fetchPage(String pageUrl) {
        // FIXME - Return a structure with all the data and error messages
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
            logger.log(Level.DEBUG, pageUrl + " Read " + content.length() + " bytes" );
            //logger.log(Level.DEBUG, content );
            return content;
        } catch (IOException ex) {
            logger.log(Level.ERROR, "I/O Exception with " + pageUrl + ": " + 
                    ex.getMessage());
            return "";
        }
    }

    private pageInfo splitPage( String page, String url ) {
        pageInfo pi = new pageInfo();
        pi.content=page; // save it all for future reference
        pi.url = url;

        Pattern p1 = Pattern.compile("<head>(.*)</head>.*"+
                "<body>(.*)", 
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m1 = p1.matcher(page);
        if (m1.find()) {
            logger.log(Level.TRACE, "P1 matched." );
            //for ( int i = 1; i<=m1.groupCount(); i++) 
            //    logger.log(Level.TRACE, "  P1 match " + i + ": '" + m1.group(i)+"'" );

            pi.headers = m1.group(1);
            pi.body = m1.group(2);
        } else {
            logger.log(Level.TRACE, "P1 did NOT match. p='" + p1.pattern() +"'" );
            pi.headers ="";
            pi.body = page; // doesn't look like good html, try to extract links anyway
        }
        pi.title="";
        Pattern p2 = Pattern.compile("<title>(.*)</title>",
                Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(pi.headers);
        if (m2.find()) {
            pi.title=m2.group(1);
        } else {
            pi.title="???"; // FIXME - try to get the first H1 tag, 
            // or first text line or something
        }
        logger.log(Level.TRACE, "Split page " + pi.url + " into "+
                "title:'" + pi.title + "'  " +
                "headers:'" + pi.headers + "' " +
                "body:'" + pi.body + "' " );

        
        return pi;
    }
    
    private void harvestLoop() {
        while (!toSearch.isEmpty()) {
            String curUrl = toSearch.firstElement();
            toSearch.removeElementAt(0);
            if ( !searched.contains(curUrl)) {
                searched.add(curUrl); // to make sure we don't go there again  
                // Fixme - check robot safe
                String page = fetchPage(curUrl);
                if (!page.isEmpty()) {
                    pageInfo pi = splitPage(page, curUrl);
                }
            }
        }
    } // harvestLoop

    public void run() {
        status = HarvestStatus.RUNNING;
        initWorkList();
        harvestLoop();
        resource.setCurrentStatus("Sleeping");
        try {
            resource.setCurrentStatus("Sleeping");
            Thread.sleep(10000);
            resource.setCurrentStatus("Woke up");
        } catch (InterruptedException ex) {
            setError("Web crawler didn't even manage to sleep!");
        }
        if (isKillSendt()) {
            setError("Web Crawl interrupted with a kill signal");
        } else {
            setError("Web Crawl not quite implemented yet");
        }
    } // run()
}

