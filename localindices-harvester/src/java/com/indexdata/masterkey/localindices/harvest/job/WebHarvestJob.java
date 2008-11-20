/* WebHarvestJob
 * Crawls around web sites and stores full text etc
 */
package com.indexdata.masterkey.localindices.harvest.job;

import com.indexdata.masterkey.localindices.entity.WebCrawlResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import java.io.IOException;
import java.io.OutputStream;
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
 * New model
 * The old one was suitable for harvesting one web site, or at most a few.
 * 
 * SiteList - a list of SiteRequests extracted from the jump page
 *   - The link to start from
 *   - Statistics for reporting
 *   - Counters for pages visited and waiting
 * 
 * Request queue 
 *   - a list of pages to be visited
 *   - Worker threads take one from there, add links to the end
 *   - contain a depth indicator, to avoid going too deep.
 * 
 * NotBefore
 *   - Mapping from hostnames to times when host can be visited
 *   - When about to visit a page, its time will be set to now+10min, so that
 *     other threads don't feel tempted to visit the same site
 *   - When a page has been processed, the time is set to now+1min, not to
 *     load the server. (note, the now is after the visiting has been done).
 *     (perhaps we add the time it took to get the page, to reduce load on
 *     heavy pages)
 * 
 * Visited
 *   - A list of pages already visited, to make sure we don't loop
 * 
 * RobotCache
 *   - Collection of robots.txt files, one for each hostname 
 * 
 * Initializing
 *   - Load the jump page, parse links, create records to start list
 *   - Start worker threads
 * 
 * Worker threads
 *   - Pick a link from the queue
 *   - If notyet, return to the end of queue (or random position near end)
 *   - Fetch page
 *   - Extract links
 *   - Append those that pass the filter, to the end of the queue
 *
 * How to get there
 *   - Init code, build start list
 *   - Request queue
 * 
 * TODO:
 *   - Parsing of the title element - only the first of many titles, not all
 *     between the first and last tags!
 *   - use proper XML tools to produce the XML fragments to index
 *   - Redirects (watch out for loops etc) (doesn't the library handle this?)
 *     (make a test page or two to see what actually happens)
 *   - different extract routines for different types
 *   - Parse and understand a jump page
 *     Start url prefixes jump: and jump2:
 *   - Keep the queue of crawlrequests, instead of urls, so we can keep track
 *     of depth limits, jumppage numbers, etc.
 *   - (after the jump page), harvest the same site only, but as deep as it goes
 *     (within some reasonable limit to avoid endless loops!)
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
 *     - or run each site in its own thread, with proper sleeps.
 *     - or, keep a last-see timestamp for each host, and check if we may call
 *       again. If not, take the request off the top of the queue, and instert
 *       into a random position in the later half of the queue.  This would be
 *       easy to do with N threads in parallel.
 *   - Detect and convert character sets, if possible 
 */
public class WebHarvestJob implements HarvestJob {
    // Data structures
    
    // Info about one harvested page
    private class pageInfo {
        public URL url = null;
        public String error = "";
        public String contenttype = "";
        public String content = ""; // the whole thing
        public String headers = "";
        public String body = "";
        public Vector<URL> links = new Vector<URL>();
        public String plaintext = "";
        public String title = "";
        public String xml = "";
    }

    // Request to harvest one site, with parameters etc
    // Collects statistics of this site, etc
    // Comes from the jump page, goes into statistics report
    private class siteRequest {
        public URL url = null; // where to start the job
        public int maxdepth =0; // how deep to recurse
        public int seen=0; // how many pages harvested
        public int togo=0; // how many to go (so far)
    }

    // Request to harvest one page
    private class pageRequest {
        public URL url = null; // the page to harvest
        public int depth=0; // how deep are we now
        public siteRequest sitereq = null; // points to the site request
    }

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
    private static WebRobotCache robotCache = new WebRobotCache();
    private final int sleepTime = 10000; // ms to sleep between requests

    
    
    public WebHarvestJob(WebCrawlResource resource) {
        this.resource = resource;
        this.status = HarvestStatus.NEW;
        logger.setLevel(Level.ALL);  // While debugging
        this.error = null;
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
        return this.storage;
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


    private void xmlStart() throws IOException {
        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" " +
                "?>\n" +
                "<records" +
                " xmlns:pz=\"http://www.indexdata.com/pazpar2/1.0\" " +
                ">\n";
        OutputStream os = storage.getOutputStream();
        os.write(header.getBytes());
    }

    private void saveXmlFragment(WebPage pi) throws IOException {
        OutputStream os = storage.getOutputStream();
        os.write(pi.xml.getBytes());
    }

    private void xmlEnd() throws IOException {
        String footer = "</records>\n";
        OutputStream os = storage.getOutputStream();
        os.write(footer.getBytes());
    }

    private String xmlTag(String tag, String data) {
        String clean = data.replaceAll("&", "&amp;"); // DIRTY - use proper XML tools
        clean = clean.replaceAll("<", "&lt;");
        clean = clean.replaceAll(">", "&gt;");
        clean = clean.replaceAll("\\s+", " ");
        return "<pz:metadata type=\"" + tag + "\">" +
                clean +
                "</pz:metadata>";
    }
    

    /** Convert the page into XML suitable for indexing with zebra */
    private void makeXmlFragment(WebPage pi) {
        // FIXME - Use proper XML tools to do this, to avoid problems with
        // bad entities, character sets, etc.
        pi.xml = "<pz:record>\n";
        pi.xml += xmlTag("md-title", pi.title);
        pi.xml += xmlTag("md-fulltext", pi.plaintext);
        pi.xml += xmlTag("md-electronic-url", pi.url.toString());
        pi.xml += "</pz:record>\n";
    } // makeXml

    private boolean filterLink(URL url, URL samepage) {
        if (samepage != null) {
            if (!url.getHost().equals(samepage.getHost())) {
                return false; // different site, don't go there
            }
        }
        if (resource.getUriMasks().isEmpty()) {
            return true; // no filtering, go anywhere
        }
        String urlStr = url.toString();
        for (String mask : resource.getUriMasks().split(" ")) {
            Pattern p = Pattern.compile(mask);
            Matcher m = p.matcher(urlStr);
            if (m.find()) {
                //logger.log(Level.TRACE, "url '"+urlStr+"' matched '" + mask + "'");
                return true;
            }
        }
        //logger.log(Level.TRACE, "url '"+urlStr+"' refused by filters");
        return false;
    }


    private void initWorkList() {
        searched = new Vector<URL>();
        toSearch = new Vector<URL>();
        nextRound = new Vector<URL>();
        logger.log(Level.TRACE, "InitWorkList: " + resource.getStartUrls());
        Pattern p = Pattern.compile("([^:]+:)?(http:[^ ]+)",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

        Matcher m = p.matcher(resource.getStartUrls());
        while (m.find()) {
            logger.log(Level.TRACE, "Start Url: " +
                    "'" + m.group(1) + "' " +
                    "'" + m.group(2) + "'");
            if (m.group(1) == null) {
                // simple http:
                try {
                    URL url;
                    url = new URL(m.group(2));
                    toSearch.add(url);
                    logger.log(Level.TRACE, "Added start URL '" + m.group(2) + "'");
                } catch (MalformedURLException ex) {
                    setError("Invalid start url: '" + m.group(2) + "'");
                    toSearch.clear();
                    return;
                }
            } else {
                if (m.group(1).equals("jump:")) {
                    try {
                        URL url;
                        url = new URL(m.group(2));
                        WebPage pi = new WebPage(url);
                        if (pi.content.isEmpty()) {
                            setError("Could not get jump page " + m.group(2) );
                            toSearch.clear();
                        } else {
                            pi.splitHtmlPage();
                            logger.log(Level.TRACE, "Jump page contained " +
                                    pi.links.size() + " HTML links");
                            if (pi.links.isEmpty()) {
                                pi.splitTextLinkPage();
                                logger.log(Level.TRACE, "Jump page contained " +
                                        pi.links.size() + " plaintext links");
                            }
                            if (pi.links.isEmpty()) {
                                setError("Jump page " + m.group(2) +
                                        " contains no links ");
                                toSearch.clear();
                                return;
                            }
                            for (URL u : pi.links) {
                                if (!toSearch.contains(u) && filterLink(u, null)) {
                                    toSearch.add(u);
                                }
                            }
                        }
                    } catch (MalformedURLException ex) {
                        setError("Invalid start url: '" + m.group(2) + "'");
                        toSearch.clear();

                    }
                } else {
                    setError("Invalid start url prefix: '" + m.group(1) + "'");
                    toSearch.clear();
                    return;
                }

            }
        }
        numtosearch = toSearch.size();
    }

    /** A little delay between making the requests
     */
    private void sleep() {
        long startTime = System.currentTimeMillis();
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ex) {
            logger.log(Level.TRACE, "Sleep got interrupted");
        }
        long elapsed = (System.currentTimeMillis() - startTime) / 1000; // sec
        if (elapsed > 2 * sleepTime) {
            logger.log(Level.TRACE, "Slept " + elapsed + "ms, " +
                    "asked for only " + sleepTime + "ms");
        }
    } // sleep

    /** Harvest one round of links */
    private void harvestRound() {
        while (!toSearch.isEmpty() && !isKillSendt()) {
            URL curUrl = toSearch.firstElement();
            toSearch.removeElementAt(0);
            if (!searched.contains(curUrl)) {
                searched.add(curUrl); // to make sure we don't go there again  
                if ( robotCache.checkRobots(curUrl)) {
                    WebPage pi = new WebPage(curUrl);
                    if (!pi.content.isEmpty()) {
                        // TODO: Split according to the type of the page
                        pi.splitHtmlPage();
                        for (URL u : pi.links) {
                            if (!nextRound.contains(u) &&
                                    filterLink(u, curUrl) &&
                                    robotCache.checkRobots(u)) {
                                nextRound.add(u);
                            }
                        }
                        makeXmlFragment(pi);
                        if (!pi.xml.isEmpty()) {
                            try {
                                saveXmlFragment(pi);
                            } catch (IOException ex) {
                                setError("I/O error writing data: " +
                                        ex.getMessage());
                            }
                        }
                        sleep();
                    } // got page
                } // robot ok
            } // not searched
        }
    } // harvestRound

    /** Harvest all there is to do */
    private void harvestLoop() {
        round = 0;
        long startTime = System.currentTimeMillis();
        initWorkList();
        while (round <= resource.getRecursionDepth() &&
                !toSearch.isEmpty() &&
                !isKillSendt() &&
                this.error == null) {
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

        String killmsg = "Did";
        if (isKillSendt()) {
            killmsg = "Killed after";
        }
        logger.log(Level.DEBUG, killmsg + " " + (round - 1) + " rounds. " +
                "Seen " + searched.size() + " urls " +
                " in " + elapsed + " seconds " +
                "(Next depth would have taken " + toSearch.size() + " pages more)");
    }

    public void run() {
        status = HarvestStatus.RUNNING;
        if (storage == null) {
            setError("Internal error: no storage set");
            return;
        }
        try {
            storage.begin();
            xmlStart();
        } catch (IOException ex) {
            setError("I/O error on storage.begin: " + ex.getMessage());
            return;
        }
        harvestLoop();
        if (this.error == null) {
            if (isKillSendt()) {
                setError("Web Crawl interrupted with a kill signal");
                try {
                    storage.rollback();
                } catch (IOException ex) {
                    setError("I/O error on storage.rollback (after interrupt) " +
                            ex.getMessage());
                }
            } else {
                try {
                    xmlEnd();
                    storage.commit();
                    status = HarvestStatus.FINISHED;
                    //setError("All done - but we call it an error so we can do again");
                } catch (IOException ex) {
                    setError("I/O error on storage.begin: " + ex.getMessage());
                }

            }
        }
    } // run()
} // class WebHarvestJob

