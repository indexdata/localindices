/* WebHarvestJob
 * Crawls around web sites and stores full text etc
 */
package com.indexdata.masterkey.localindices.harvest.job;

import com.indexdata.masterkey.localindices.crawl.CrawlQueue;
import com.indexdata.masterkey.localindices.entity.WebCrawlResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;

import com.indexdata.masterkey.localindices.crawl.SiteRequest;
import com.indexdata.masterkey.localindices.crawl.PageRequest;
import com.indexdata.masterkey.localindices.crawl.HTMLPage;
import com.indexdata.masterkey.localindices.crawl.WebRobotCache;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
 *   - Refactor a bit
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

    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
    private HarvestStorage storage;
    private HarvestStatus status;
    private String error;
    private WebCrawlResource resource;
    private boolean die = false;
    private Vector<SiteRequest> sites;
    private CrawlQueue que;
    private int round;
    private static WebRobotCache robotCache = new WebRobotCache();
    private final int sleepTime = 10000; // ms to sleep between requests
    private final int hitInterval = 60 * 1000;  // ms between hitting the same host

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
        saveXmlFragment(header);
    }

    private void saveXmlFragment(String xml) throws IOException {
        OutputStream os = storage.getOutputStream();
        os.write(xml.getBytes());
    }

    private void xmlEnd() throws IOException {
        String footer = "</records>\n";
        saveXmlFragment(footer);
    }

    /** Split a plain-text link page
     */
    private List<URL> splitTextLinkPage(HTMLPage page) {
        Long startTime = System.currentTimeMillis();
        //List<URL> links = new Vector<URL>();
        List<URL> links = new ArrayList<URL>(100);
        //Pattern p = Pattern.compile("(http://\\S+)",
        Pattern p = Pattern.compile("(http://[^ <>]+)",
                Pattern.CASE_INSENSITIVE);
        String body = page.getBody();
        Matcher m = p.matcher(body);
        URL pgUrl = page.getUrl();
        logger.log(Level.TRACE, "Parsing text links from " + pgUrl.toString() + " : " +
                body.length() + "bytes " + trunc(body, 50));
        while (m.find()) {
            String lnk = m.group(1);
            URL linkUrl = null;
            if (lnk != null) {
                try {
                    logger.log(Level.TRACE, "Anout to do " + lnk);
                    linkUrl = new URL(pgUrl, lnk);
                    if (linkUrl == null) {
                        logger.log(Level.TRACE, "OOPS Got a null URL");
                    }
                    logger.log(Level.TRACE, "Found link '" + lnk + "' " +
                            "-> '" + linkUrl.toString() + "'");
                    /* NOTE - this is awfully slow - so we don't deduplicate here
                     * It will happe in the work queue anyway.
                     * See the comment on HTMLPage
                    if (!links.contains(linkUrl)) {
                        links.add(linkUrl);
                    }
                     */
                   links.add(linkUrl);
                   logger.log(Level.TRACE, "Added into links");
                } catch (MalformedURLException ex) {
                    logger.log(Level.TRACE, "Could not make a good url from " +
                            "'" + lnk + "' " +
                            "when parsing " + page.getUrl().toString());
                }
            }
        }
        Long elapsed = System.currentTimeMillis() - startTime;
        logger.log(Level.TRACE, "Parsed " + links.size() + " links in " +
                elapsed + " ms");

        return links;
    }

    private String trunc(String s, int len) {
        if (s.length() <= len) {
            return s;
        }
        return s.substring(0, len - 1);
    }

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
        sites = new Vector<SiteRequest>();
        que = new CrawlQueue();
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
                    SiteRequest site = new SiteRequest();
                    site.url = new URL(m.group(2));
                    site.maxdepth = resource.getRecursionDepth();
                    sites.add(site);
                    logger.log(Level.TRACE, "Added start URL '" + m.group(2) + "'" +
                            " (d=" + site.maxdepth + ")");
                } catch (MalformedURLException ex) {
                    setError("Invalid start url: '" + m.group(2) + "'");
                    sites.clear();
                    return;
                }
            } else {
                if (m.group(1).equals("jump:")) {
                    try {
                        URL url;
                        url = new URL(m.group(2));
                        HTMLPage pi = new HTMLPage(url);
                        if (pi.getContent() == null || pi.getContent().isEmpty()) {
                            setError("Could not get jump page " + m.group(2));
                            sites.clear();
                        } else {
                            List<URL> links = pi.getLinks();
                            logger.log(Level.TRACE, "Jump page contained " +
                                    links.size() + " HTML links");
                            if (links.isEmpty()) {
                                links = splitTextLinkPage(pi);
                                logger.log(Level.TRACE, "Jump page contained " +
                                        links.size() + " plaintext links");
                            }
                            if (links.isEmpty()) {
                                setError("Jump page " + m.group(2) +
                                        " contains no links ");
                                sites.clear();
                                return;
                            }
                            for (URL u : links) {
                                SiteRequest site = new SiteRequest();
                                site.url = u;
                                site.maxdepth = resource.getRecursionDepth();
                                if (sites.contains(site)) {
                                    logger.log(Level.INFO, "Site " + u.toString() +
                                            " is already in the jump list.");
                                } else {
                                    sites.add(site);
                                    logger.log(Level.INFO, "Added jump link " + u.toString());
                                }
                            }
                        }
                    } catch (MalformedURLException ex) {
                        setError("Invalid start url: '" + m.group(2) + "'");
                        sites.clear();

                    } catch (IOException ex) {
                        setError("I/O Exception '" + m.group(2) + "'" + ex.getMessage());
                        sites.clear();
                    }
                } else {
                    setError("Invalid start url prefix: '" + m.group(1) + "'");
                    sites.clear();
                    return;
                }

            }
        }
        logger.log(Level.INFO, "Starting with " + sites.size() + " start links");
        for (SiteRequest s : sites) {
            que.add(s);
        }
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

    /** Harvest one round of links - NO, the whole queue, until we move to threads*/
    private void harvestRound() {
        while (!que.isEmpty() && !isKillSendt()) {
            PageRequest pg = que.get();
            if (pg == null) {
                logger.log(Level.TRACE, "Got a null pagerequest, must be done");
                return;
            }
            URL curUrl = pg.url;
            if (robotCache.checkRobots(curUrl)) {
                HTMLPage pi = null;
                try {
                    pi = new HTMLPage(curUrl);
                    que.setNotYet(pg, hitInterval);
                } catch (IOException ex) {
                    logger.log(Level.TRACE, "I/O error in getting " +
                            curUrl.toString() + " : " + ex.getMessage());
                }
                if (pi != null && pi.getContent() != null &&
                        !pi.getContent().isEmpty()) {
                    for (URL u : pi.getLinks()) {
                        if (filterLink(u, curUrl) &&
                                robotCache.checkRobots(u)) {
                            que.add(pg, u);
                        }
                    }
                    String xml = pi.toPazpar2Metadata();
                    if (!xml.isEmpty()) {
                        try {
                            saveXmlFragment(xml);
                        } catch (IOException ex) {
                            setError("I/O error writing data: " +
                                    ex.getMessage());
                        }
                    }
                } // got page
            } // robot ok
        }
    } // harvestRound

    /** Harvest all there is to do */
    private void harvestLoop() {
        round = 0;
        long startTime = System.currentTimeMillis();
        initWorkList();
        harvestRound();
        /*
        while (round <= resource.getRecursionDepth() &&
        !que.isEmpty() &&
        !isKillSendt() &&
        this.error == null) {
        logger.log(Level.DEBUG, "Round " + round + ": " +
        searched.size() + " urls seen. " +
        nextRound.size() + " urls to go ");
        toSearch.addAll(nextRound);
        numtosearch += toSearch.size();
        nextRound.clear();
        round++;
        }
         */
        long elapsed = (System.currentTimeMillis() - startTime) / 1000; // sec

        String killmsg = "Did";
        if (isKillSendt()) {
            killmsg = "Killed after";
        }
        logger.log(Level.DEBUG, killmsg + " " + (round - 1) + " rounds. " +
                "Seen " + que.numSeen() + " urls " +
                " in " + elapsed + " seconds ");
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

