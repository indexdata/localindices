/* WebHarvestJob
 * Crawls around web sites and stores full text etc
 */
package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.crawl.CrawlQueue;
import com.indexdata.masterkey.localindices.crawl.CrawlThread;
import com.indexdata.masterkey.localindices.crawl.HTMLPage;
import com.indexdata.masterkey.localindices.crawl.SiteRequest;
import com.indexdata.masterkey.localindices.crawl.WebRobotCache;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.entity.WebCrawlResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.harvest.storage.Pz2SolrRecordContentHandler;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.TransformationChainRecordStorageProxy;

/**
 * WebHarvestJob Crawls around web sites and stores full text, title, url, etc.
 * 
 * @author heikki
 * 
 *         New model The old one was suitable for harvesting one web site, or at
 *         most a few.
 * 
 *         SiteList - a list of SiteRequests extracted from the jump page - The
 *         link to start from - Statistics for reporting - Counters for pages
 *         visited and waiting
 * 
 *         Request queue - a list of pages to be visited - Worker threads take
 *         one from there, add links to the end - contain a depth indicator, to
 *         avoid going too deep.
 * 
 *         NotBefore - Mapping from hostnames to times when host can be visited
 *         - When about to visit a page, its time will be set to now+10min, so
 *         that other threads don't feel tempted to visit the same site - When a
 *         page has been processed, the time is set to now+1min, not to load the
 *         server. (note, the now is after the visiting has been done). (perhaps
 *         we add the time it took to get the page, to reduce load on heavy
 *         pages)
 * 
 *         Visited - A list of pages already visited, to make sure we don't loop
 * 
 *         RobotCache - Collection of robots.txt files, one for each hostname
 * 
 *         Initializing - Load the jump page, parse links, create records to
 *         start list - Start worker threads
 * 
 *         Worker threads - Pick a link from the queue - If notyet, return to
 *         the end of queue (or random position near end) - Fetch page - Extract
 *         links - Append those that pass the filter, to the end of the queue
 * 
 *         How to get there - Refactor a bit - Init code, build start list -
 *         Request queue
 * 
 *         TODO: - Parsing of the title element - only the first of many titles,
 *         not all between the first and last tags! - use proper XML tools to
 *         produce the XML fragments to index - Redirects (watch out for loops
 *         etc) (doesn't the library handle this?) (make a test page or two to
 *         see what actually happens) - different extract routines for different
 *         types - Parse and understand a jump page Start url prefixes jump: and
 *         jump2: - Keep the queue of crawlrequests, instead of urls, so we can
 *         keep track of depth limits, jumppage numbers, etc. - (after the jump
 *         page), harvest the same site only, but as deep as it goes (within
 *         some reasonable limit to avoid endless loops!) - Parameters - Remove
 *         the filetype mask - Add max pages to harvest TODO - tests - Test a
 *         site with plain text files - Test with a tarpit-like site TODO - but
 *         in some later version - robots.txt - check user-agent lines -
 *         understand also Allow lines - better load reduction for servers. -
 *         keep todo list in a priority queue, with time stamps when a hosts
 *         turn is - or run each site in its own thread, with proper sleeps. -
 *         or, keep a last-see timestamp for each host, and check if we may call
 *         again. If not, take the request off the top of the queue, and instert
 *         into a random position in the later half of the queue. This would be
 *         easy to do with N threads in parallel. - Detect and convert character
 *         sets, if possible
 */
public class WebRecordHarvestJob extends AbstractRecordHarvestJob implements WebHarvestJobInterface {

  //private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
  // private HarvestStorage storage;
  private Proxy proxy;
  private WebCrawlResource resource;
  private boolean die = false;
  private Vector<SiteRequest> sites;
  private CrawlQueue que;
  private WebRobotCache robotCache;
  private final int hitInterval = 60 * 1000; // ms between hitting the same host
  private final int minNumWorkers = 20;
  private final int maxNumWorkers = 100;
  private Vector<CrawlThread> workers = new Vector<CrawlThread>(maxNumWorkers);

  public WebRecordHarvestJob(WebCrawlResource resource, Proxy proxy) {
    this.resource = resource;
    this.proxy = proxy;
    robotCache = new WebRobotCache(proxy);
    // logger.setLevel(Level.ALL); // While debugging
    this.error = null;
    setStatus(HarvestStatus.valueOf(resource.getCurrentStatus()));
    logger = new StorageJobLogger(getClass(), resource);
    List<TransformationStep> steps = resource.getTransformation().getSteps();
    setupTemplates(resource, steps);
  }

  private synchronized boolean isKillSendt() {
    if (die) {
      logger.log(Level.WARN, "Web harvest received kill signal.");
    }
    return die;
  }

  @Override
  public void setStorage(HarvestStorage storage) {
    if (storage instanceof RecordStorage) {
      super.setStorage((RecordStorage) storage);
    }
    else {
      setStatus(HarvestStatus.ERROR);
      resource.setCurrentStatus("Unsupported StorageType: " + storage.getClass().getCanonicalName()
	  + ". Requires RecordStorage");
    }
  }

  public WebRobotCache getRobotCache() {
    return robotCache;

  }

  // Set an "error" message to report progress
  public synchronized void setStatusMsg(String e) {
    if (getStatus() == HarvestStatus.RUNNING) {
      resource.setMessage(e);
      error = e;
      // logger.log(Level.TRACE, "Reporting status " + e);
    } else {
      // logger.log(Level.TRACE, "NOT Reporting status " + e + ". not running");
    }
  }

  public synchronized void setError(String e) {
    this.error = e;
    setStatus(HarvestStatus.ERROR);
    resource.setMessage(e);
    logger.log(Level.ERROR, e);
    if (que != null) {
      que.setFinished();
    }
  }

  public synchronized String getMessage() {
    return error;
  }

  private void xmlStart() throws IOException {
    String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" " + "?>\n" + "<records"
	+ " xmlns:pz=\"http://www.indexdata.com/pazpar2/1.0\" " + ">\n";
    saveXmlFragment(header);
  }

  public synchronized void saveXmlFragment(String xml) throws IOException {
    OutputStream os = getOutputStream();
    os.write(xml.getBytes());
  }

  private void xmlEnd() throws IOException {
    String footer = "</records>\n";
    saveXmlFragment(footer);
  }

  /**
   * Split a plain-text link page
   */
  private List<URL> splitTextLinkPage(HTMLPage page) {
    Long startTime = System.currentTimeMillis();
    // List<URL> links = new Vector<URL>();
    List<URL> links = new ArrayList<URL>(100);
    // Pattern p = Pattern.compile("(http://\\S+)",
    Pattern p = Pattern.compile("(http://[^ <>]+)", Pattern.CASE_INSENSITIVE);
    String body = page.getContent();
    Matcher m = p.matcher(body);
    URL pgUrl = page.getUrl();
    logger.log(Level.TRACE, "Parsing text links from " + pgUrl.toString() + " : " + body.length()
	+ "bytes " + trunc(body, 50));
    while (m.find()) {
      String lnk = m.group(1);
      URL linkUrl = null;
      if (lnk != null) {
	try {
	  linkUrl = new URL(pgUrl, lnk);
	  logger.log(Level.TRACE, "Found link '" + lnk + "' " + "-> '" + linkUrl.toString() + "'");
	  /*
	   * NOTE - Vector.contains() is awfully slow - so we don't deduplicate
	   * here It will happen in the work queue anyway. See the comment on
	   * HTMLPage if (!links.contains(linkUrl)) { links.add(linkUrl); }
	   */
	  links.add(linkUrl);
	} catch (MalformedURLException ex) {
	  logger.log(Level.TRACE, "Could not make a good url from " + "'" + lnk + "' "
	      + "when parsing " + page.getUrl().toString());
	}
      }
    }
    Long elapsed = System.currentTimeMillis() - startTime;
    logger.log(Level.TRACE, "Parsed " + links.size() + " links in " + elapsed + " ms");

    return links;
  }

  private String trunc(String s, int len) {
    if (s.length() <= len) {
      return s;
    }
    return s.substring(0, len - 1);
  }

  private void initWorkList() {
    sites = new Vector<SiteRequest>();
    que = new CrawlQueue();
    logger.log(Level.TRACE, "InitWorkList: " + resource.getStartUrls());
    Pattern p = Pattern.compile("[ ]*([^:]+:)?(http:[^ ]+)", Pattern.CASE_INSENSITIVE
	| Pattern.MULTILINE);

    Matcher m = p.matcher(resource.getStartUrls());
    while (m.find()) {
      logger.log(Level.TRACE, "Start Url: " + "'" + m.group(1) + "' " + "'" + m.group(2) + "'");
      if (m.group(1) == null) {
	// simple http:
	try {
	  SiteRequest site = new SiteRequest();
	  site.url = new URL(m.group(2));
	  site.maxdepth = resource.getRecursionDepth();
	  sites.add(site);
	  logger.log(Level.TRACE, "Added start URL '" + m.group(2) + "'" + " (d=" + site.maxdepth + ")");
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
	    HTMLPage pi = new HTMLPage(url, proxy);
	    if (pi.getContent() == null || pi.getContent().isEmpty()) {
	      setError("Could not get jump page " + m.group(2));
	      sites.clear();
	    } else {
	      List<URL> links = pi.getLinks();
	      logger.log(Level.TRACE, "Jump page contained " + links.size() + " HTML links");
	      if (links.isEmpty()) {
		links = splitTextLinkPage(pi);
		logger.log(Level.TRACE, "Jump page contained " + links.size() + " plaintext links");
	      }
	      if (links.isEmpty()) {
		setError("Jump page " + m.group(2) + " contains no links ");
		sites.clear();
		return;
	      }
	      for (URL u : links) {
		SiteRequest site = new SiteRequest();
		site.url = u;
		site.maxdepth = resource.getRecursionDepth() != null ? resource.getRecursionDepth()
		    : 1;
		if (sites.contains(site)) {
		  logger.log(Level.INFO, "Site " + u.toString() + " is already in the jump list.");
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
	    // setError("I/O Exception '" + m.group(2) + "'" + ex.getMessage());
	    setError("Could not load jump page " + m.group(2));
	    sites.clear();
	  }
	} else {
	  setError("Invalid start url prefix: '" + m.group(1) + "'");
	  sites.clear();
	  return;
	}

      }
    }
    for (SiteRequest s : sites) {
      que.add(s);
    }
    logger.log(Level.INFO, "Starting with " + que.size() + " start links " + "from " + sites.size()
	+ " jump links");
  }

  private void logWorkerStatus() {

    String s = "";
    s += resource.getName();
    s += " todo=" + que.size();
    s += " seen=" + que.numSeen();
    s += " done=" + (que.numSeen() - que.size());
    s += " working=" + que.getUnderWork();
    s += " S:";
    for (CrawlThread th : workers) {
      s += "" + th.getStatus();
    }
    logger.log(Level.DEBUG, s);
    setStatusMsg("" + (que.numSeen() - que.size()) + " pages harvested,"
	+ (que.size() + que.getUnderWork()) + " to go ");

  }

  /** Harvest all there is to do */
  private void harvestLoop() {
    long startTime = System.currentTimeMillis();
    initWorkList();

    int nw = que.size() / 10;
    if (nw > maxNumWorkers) {
      nw = maxNumWorkers;
    }
    if (nw < minNumWorkers) {
      nw = minNumWorkers;
    }
    logger.log(Level.DEBUG, "Starting " + nw + " threads");
    for (int i = 0; i < nw; i++) {
      // logger.log(Level.DEBUG, "Starting thread " + i + " of " + nw);
      CrawlThread worker = new CrawlThread(this, proxy, que, "", i, hitInterval);
      Thread wthread = new Thread(worker);
      workers.add(worker);
      wthread.start();
    }
    logger.log(Level.DEBUG, "Started threads OK");
    while (!que.alldone()) {
      try {
	Thread.sleep(30 * 1000);
      } catch (InterruptedException ex) {
	logger.log(Level.DEBUG, "Sleep interrupted, never mind");
      }
      logWorkerStatus();
    }

    long elapsed = (System.currentTimeMillis() - startTime) / 1000; // sec
    String killmsg = "Ok!";
    if (isKillSendt()) {
      killmsg = "Killed!";
      // resource.setError("Interruped");
    } else {
      // resource.setError("OK. " + que.numSeen() + " pages harvested");
    }
    logger.log(Level.DEBUG, killmsg + " " + "Seen " + que.numSeen() + " urls " + " in " + elapsed
	+ " seconds ");
  }

  public void run() {
    setStatus(HarvestStatus.RUNNING);
    setStatusMsg("");
    if (getStorage() == null) {
      setError("Internal error: no storage set");
      return;
    }
    try {
      getStorage().begin();
      getStorage().databaseStart(resource.getId().toString(), null);
      xmlStart();
    } catch (IOException ex) {
      setError("I/O error on storage.begin: " + ex.getMessage());
      return;
    }
    harvestLoop();
    if (getStatus() == HarvestStatus.RUNNING) {
      if (isKillSendt()) {
	setError("Web Crawl interrupted with a kill signal");
	try {
	  getStorage().rollback();
	} catch (IOException ex) {
	  setError("I/O error on storage.rollback (after interrupt) " + ex.getMessage());
	}
      } else {
	try {
	  xmlEnd();
	  getStorage().commit();
	  resource.setMessage("OK. " + que.numSeen() + " pages harvested");
	  setStatus(HarvestStatus.FINISHED);
	  // setError("All done - but we call it an error so we can do again");
	} catch (IOException ex) {
	  setError("I/O error on storage.commit: " + ex.getMessage());
	}

      }
    }
  } // run()

  public RecordStorage setupTransformation(RecordStorage storage) {
    if (resource.getTransformation() != null && resource.getTransformation().getSteps().size() > 0) {
      XMLReader xmlReader;
      try {
	xmlReader = createTransformChain(false);
	return new TransformationChainRecordStorageProxy(storage, xmlReader,
	    new Pz2SolrRecordContentHandler(storage, resource.getId().toString()), logger);

      } catch (Exception e) {
	e.printStackTrace();
	logger.error(e.getMessage());
      }
    }
    logger.warn("No Transformation Proxy configured.");
    return storage;
  }

  
  @Override
  public OutputStream getOutputStream() {
    return setupTransformation(getStorage()).getOutputStream();
  }
} // class WebHarvestJob

