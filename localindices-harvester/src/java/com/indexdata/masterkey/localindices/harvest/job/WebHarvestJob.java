/* WebHarvestJob
 * Crawls around web sites and stores full text etc
 */
package com.indexdata.masterkey.localindices.harvest.job;

import com.indexdata.masterkey.localindices.entity.WebCrawlResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 * New model
 * The old one was suitable for harvesting one web site, or at most a few.
 * 
 * Start list - a list of links extracted from the jump page
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
    private Vector<URL> toSearch; // todo list for this round
    private Vector<URL> searched; // all pages we have seen
    private Vector<URL> nextRound; // links found in this round are pushed here
    private int numtosearch;
    private int round;
    private Map<URL, String> robotCache = new HashMap<URL, String>();
    private final static int connTimeOut = 30000; // ms to make a connection
    private final static int readTimeOut = 30000; // ms to read a block
    // About 30 seconds seems reasonable. 
    private final static int readBlockSize = 1000000; // bytes to read in one op
    private final static int maxReadSize = 10000000; // 10MB 
    private final static String userAgentString = "IndexData Masterkey Web crawler";
    private final int sleepTime = 10000; // ms to sleep between requests

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

    private pageInfo fetchPage(URL url) {
        // FIXME - Pass a request structure that contains the URL,
        // and also referring page, jumppage-flag, and other stuff.

        pageInfo pi = new pageInfo();
        pi.url = url;
        pi.error = null;
        pi.content = "";
        String pageUrl = url.toString();
        logger.log(Level.TRACE, "About to fetch '" + pageUrl + "'");
        if (url.getProtocol().compareTo("http") != 0) {
            pi.error = "Unsupported protocol in '" + pageUrl + "'";
            logger.log(Level.ERROR, pi.error);
            return pi;
        }
        try {
            URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(connTimeOut);
            urlConnection.setReadTimeout(readTimeOut);
            urlConnection.setAllowUserInteraction(false);
            urlConnection.setRequestProperty("User-agent", userAgentString);
            InputStream urlStream = url.openStream();
            pi.contenttype = urlConnection.getContentType();
            // Fixme - this requests the page once! And with 'Java' in user'agent
            // and below we fetch it once more! (with proper user-agent)
            if (pi.contenttype == null || pi.contenttype.isEmpty()) {
                pi.contenttype = URLConnection.guessContentTypeFromStream(urlStream);
            }
            if (pi.contenttype == null || pi.contenttype.isEmpty()) {
                pi.error = "Skipped '" + pageUrl + "'. could not get content type";
                logger.log(Level.DEBUG, pi.error);
                return pi;
            }
            if (!pi.contenttype.startsWith("text/html") &&
                    !pi.contenttype.startsWith("text/plain")) {
                // Get also plain text, we need it for robots.txt, and
                // might as well index it all anyway
                pi.error = "Skipped '" + pageUrl + "'. Content type '" +
                        pi.contenttype + "' not acceptable ";
                logger.log(Level.DEBUG, pi.error);
                return pi;
            }
            // search the input stream for links
            // first, read in the entire URL
            byte b[] = new byte[readBlockSize];
            int numRead = urlStream.read(b);
            if (numRead <= 0) {
                pi.content = "";
            } else {
                pi.content = new String(b, 0, numRead);
                while ((numRead != -1) && (pi.content.length() < maxReadSize)) {
                    numRead = urlStream.read(b);
                    if (numRead != -1) {
                        String newContent = new String(b, 0, numRead);
                        pi.content += newContent;
                    }
                }
            }
            urlStream.close();
            logger.log(Level.TRACE, pageUrl + " Read " + pi.content.length() + " bytes");
            //logger.log(Level.DEBUG, content );
            return pi;
        } catch (FileNotFoundException ex) {
            pi.error = "I/O Exception: " + pageUrl + " Not found ";
        // FIXME - Display also the referring page
        } catch (IOException ex) {
            pi.error = "I/O Exception " +
                    "(" + ex.getClass().getSimpleName() + ") " +
                    "with " + pageUrl + ": " + ex.getMessage();
            logger.log(Level.ERROR,
                    "I/O Exception " +
                    "(" + ex.getClass().getSimpleName() + ") " +
                    "with " + pageUrl + ": " + ex.getMessage());
        }
        logger.log(Level.DEBUG, pi.error);
        return pi;
    } // fetchPage

    /** Split a html page. 
     * First extract body and headers, then fulltext and links 
     */
    private void splitHtmlPage(pageInfo pi) {
        // Split headers and body, if possible
        Pattern p = Pattern.compile("<head>(.*)</head>.*" +
                "<body[^>]*>(.*)",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m = p.matcher(pi.content);
        if (m.find()) {
            //logger.log(Level.TRACE, "P1 matched.");
            pi.headers = m.group(1);
            pi.body = m.group(2);
        } else {
            logger.log(Level.TRACE, "P1 did NOT match. p='" + p.pattern() + "'");
            pi.headers = "";
            pi.body = pi.content; // doesn't look like good html, try to extract links anyway
        }
        // Extract a title
        pi.title = "";
        p = Pattern.compile("<title>\\s*(.*\\S)??\\s*</title>",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
                // The ?? modifier should make it reluctant, so we get the firs title
                // only, if there are several FIXME - does not work
        m = p.matcher(pi.headers);
        if (m.find() && m.group(1) != null && !m.group(1).isEmpty()) {
            pi.title = m.group(1);
            // FIXME - truncate to a decent max
        } else {
            pi.title = "???"; // FIXME - try to get the first H1 tag, 
        // or first text line or something
        }

        // extract full text
        p = Pattern.compile("<[^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        m = p.matcher(pi.content);
        pi.plaintext = m.replaceAll("");
        //logger.log(Level.TRACE, "Plaintext: " + pi.plaintext);

        // extract links
        p = Pattern.compile("<a[^>]+href=['\"]?([^>'\"#]+)['\"# ]?[^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        m = p.matcher(pi.body);
        while (m.find()) {
            String lnk = m.group(1);
            URL linkUrl;
            try {
                linkUrl = new URL(pi.url, lnk);
            } catch (MalformedURLException ex) {
                logger.log(Level.TRACE, "Could not make a good url from " +
                        "'" + lnk + "' " +
                        "when parsing " + pi.url.toString());
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
                round + ":" + searched.size() + "/" + numtosearch + " " +
                pi.url + " " +
                "title:'" + pi.title + "' (" +
                "h=" + pi.headers.length() + "b " +
                "b=" + pi.body.length() + "b) " +
                pi.links.size() + " links");
    } // splitHtmlPage

    /** Split a plain-text link page
     */
    private void splitTextLinkPage(pageInfo pi) {
        pi.links.clear();
        //Pattern p = Pattern.compile("(http://\\S+)",
        Pattern p = Pattern.compile("(http://[^ <>]+)",
                Pattern.CASE_INSENSITIVE );
        Matcher m = p.matcher(pi.body);
        logger.log(Level.TRACE, "Parsing text links from " +
                pi.body.length() + "bytes " + trunc(pi.body,50) );
        while (m.find()) {
            String lnk = m.group(1);
            URL linkUrl;
            try {
                linkUrl = new URL(pi.url, lnk);
            } catch (MalformedURLException ex) {
                logger.log(Level.TRACE, "Could not make a good url from " +
                        "'" + lnk + "' " +
                        "when parsing " + pi.url.toString());
                break;
            }
            logger.log(Level.TRACE, "Found link '" + m.group(1) + "' '" + lnk + "' " +
                    "-> '" + linkUrl.toString() + "'");
            if (!pi.links.contains(linkUrl)) {
                pi.links.add(linkUrl);
            }
        }
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

    private void saveXmlFragment(pageInfo pi) throws IOException {
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
    
    private String trunc (String s, int len) {
        if (s.length()<=len)
            return s;
        return s.substring(0,len-1);
    }

    /** Convert the page into XML suitable for indexing with zebra */
    private void makeXmlFragment(pageInfo pi) {
        // FIXME - Use proper XML tools to do this, to avoid problems with
        // bad entities, character sets, etc.
        pi.xml = "<pz:record>\n";
        pi.xml += xmlTag("md-title", pi.title);
        pi.xml += xmlTag("md-fulltext", pi.plaintext);
        pi.xml += xmlTag("md-electronic-url", pi.url.toString());
        pi.xml += "</pz:record>\n";
    } // makeXml

    private boolean checkRobots(URL url) {
        String strHost = url.getHost();
        if (strHost == null || strHost.isEmpty()) {
            // Should not happen!
            logger.log(Level.DEBUG, "Could not extract host from '" +
                    url.toString() + "' - skipping robots txt");
            return false;
        }
        String strRobot = "http://" + strHost + "/robots.txt";
        URL robUrl;
        try {
            robUrl = new URL(strRobot);
        } catch (MalformedURLException e) {
            // something weird is happening, so don't trust it
            logger.log(Level.DEBUG, "Could not create robot url " +
                    "'" + strRobot + "'");
            return false;
        }
        String robtxt = robotCache.get(robUrl);
        if (robtxt == null) {
            pageInfo robpg = fetchPage(robUrl);
            robtxt = robpg.content;
            robotCache.put(robUrl, robtxt);
            logger.log(Level.DEBUG, "Got " + robUrl.toString() +
                    " (" + robtxt.length() + " b)");
        }
        if (robtxt.isEmpty()) {
            return true; // no robots.txt, go ahead
        // Simplified, we assume all User-agent lines apply to us
        // Most likely nobody has (yet?) written a robots.txt section 
        // for specifically for us.
        }
        Pattern p = Pattern.compile("^Disallow:\\s*(.*\\S)\\s*$",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher m = p.matcher(robtxt);
        String urlpath = url.getPath();
        while (m.find()) {
            String path = m.group(1);
            if (urlpath.startsWith(path)) {
                logger.log(Level.TRACE, "Path '" + urlpath + "' forbidden " +
                        "by robot '" + path + "'");
                return false; // found one they don't want us to go to
            }
        }
        //logger.log(Level.TRACE, "Path '"+urlpath+"' all right ");
        return true;
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

    private void OLDinitWorkList() {
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
                setError("Invalid start url: '" + startUrl + "'");
                toSearch.clear();
            }
        // Fixme - validate they all are OK
        }
        numtosearch = toSearch.size();
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
                        pageInfo pi = fetchPage(url);
                        if (!pi.content.isEmpty()) {
                            splitHtmlPage(pi);
                            logger.log(Level.TRACE, "Jump page contained " +
                                    pi.links.size() + " HTML links");
                            if (pi.links.isEmpty()) {
                                splitTextLinkPage(pi);
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
                if (checkRobots(curUrl)) {
                    pageInfo pi = fetchPage(curUrl);
                    if (!pi.content.isEmpty()) {
                        // TODO: Split according to the type of the page
                        splitHtmlPage(pi);
                        for (URL u : pi.links) {
                            if (!nextRound.contains(u) &&
                                    filterLink(u, curUrl) &&
                                    checkRobots(u)) {
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

