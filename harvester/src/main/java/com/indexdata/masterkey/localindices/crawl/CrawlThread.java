/*
 */
package com.indexdata.masterkey.localindices.crawl;

import com.indexdata.masterkey.localindices.harvest.job.WebHarvestJobInterface;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * A crawler working thread Crawls pages from the queue, until told to stop If
 * queue is empty, sleeps a few seconds and tries again, in case something was
 * added
 * 
 * @author heikki
 */
public class CrawlThread implements Runnable {

  public static final int CRAWLTHREAD_STARTING = 0;
  public static final int CRAWLTHREAD_QWAIT = 1;
  public static final int CRAWLTHREAD_IOWAIT = 2;
  public static final int CRAWLTHREAD_PROCESSING = 3;
  public static final int CRAWLTHREAD_SAVING = 4;
  public static final int CRAWLTHREAD_DONE = 5;
  private static Logger logger = Logger
      .getLogger("com.indexdata.masterkey.localindices.crawl");
  WebHarvestJobInterface job;
  private Proxy proxy;
  private CrawlQueue que;
  private String filterString;
  private int threadNumber;
  private int hitInterval;
  private int status = CRAWLTHREAD_STARTING;

  public CrawlThread(WebHarvestJobInterface job, Proxy proxy, CrawlQueue que,
      String filterString, int threadNumber, int hitInterval) {
    this.job = job;
    this.proxy = proxy;
    this.que = que;
    this.filterString = filterString;
    this.threadNumber = threadNumber;
    this.hitInterval = hitInterval;
  }

  public synchronized int getStatus() {
    return status;
  }

  private synchronized void setStatus(int status) {
    this.status = status;
    // logger.log(Level.TRACE,"thread " + threadNumber + " set status to " +
    // status );
  }

  private boolean filterLink(URL url, URL samepage) {
    if (samepage != null) {
      if (!url.getHost().equals(samepage.getHost())) {
	return false; // different site, don't go there
      }
    }
    if (filterString.isEmpty()) {
      return true; // no filtering, go anywhere
    }
    String urlStr = url.toString();
    for (String mask : filterString.split(" ")) {
      Pattern p = Pattern.compile(mask);
      Matcher m = p.matcher(urlStr);
      if (m.find()) {
	// logger.log(Level.TRACE, "url '"+urlStr+"' matched '" + mask + "'");
	return true;
      }
    }
    // logger.log(Level.TRACE, "url '"+urlStr+"' refused by filters");
    return false;
  }

  private void crawlPage(PageRequest pg) {
    URL curUrl = pg.url;
    if (job.getRobotCache().checkRobots(curUrl)) {
      HTMLPage pi = null;
      try {
	setStatus(CRAWLTHREAD_IOWAIT);
	pi = new HTMLPage(curUrl, proxy);
	setStatus(CRAWLTHREAD_PROCESSING);
	que.setNotYet(pg, hitInterval);
	// TODO: Time how long it took to get a page, and add that
	// to the interval, not to overload servers that are heavy
      } catch (IOException ex) {
	que.setNotYet(pg, hitInterval); // Could be a different interval
	logger.log(
	    Level.TRACE,
	    "Thread " + threadNumber + ": I/O error in getting "
		+ curUrl.toString() + " : " + ex.getMessage());
      }
      if (pi != null && pi.getContent() != null && !pi.getContent().isEmpty()) {
	// FIXME - Ought to check the depth limit already here,
	// and skip the whole link rumba, if at a leaf node.
	// Even better, if HTMLpage would extract links in a lazy way,
	// only when needed.

	for (URL u : pi.getLinks()) {
	  if (filterLink(u, curUrl) && job.getRobotCache().checkRobots(u)) {
	    que.add(pg, u);
	  }
	}
	String xml = pi.toPazpar2Metadata();
	if (!xml.isEmpty()) {
	  try {
	    setStatus(CRAWLTHREAD_SAVING);
	    job.getOutputStream().write(xml.getBytes());
	    setStatus(CRAWLTHREAD_PROCESSING);
	  } catch (IOException ex) {
	    job.setError("I/O error writing data: " + ex.getMessage());
	    logger.log(Level.TRACE, xml);
	  }
	}
      } // got page
    } // robot ok
    /*
     * else { logger.log(Level.TRACE, "Thread " + threadNumber +
     * ": Robots NOT OK for  " + curUrl.toString());
     * 
     * }
     */

  }

  public void run() {
    setStatus(CRAWLTHREAD_STARTING);
    logger.log(Level.TRACE, "thread " + threadNumber + " starting");
    PageRequest pg;
    setStatus(CRAWLTHREAD_QWAIT);
    while ((pg = que.get()) != null) {
      setStatus(CRAWLTHREAD_PROCESSING);
      crawlPage(pg);
      que.decrementUnderWork(); // ok, now we are finished with it
      setStatus(CRAWLTHREAD_QWAIT);
    }
    setStatus(CRAWLTHREAD_DONE);
    logger.log(Level.TRACE, "thread " + threadNumber + " finished");
  } // run
} // class Crawlthread
