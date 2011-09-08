/* Work queue for the crawler
 * 
 *  Copyright (c) 2008, Index Data
 *  All rights reserved.
 *  See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.crawl;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Work queue for the crawler
 * 
 * A fairly simple queue structure.
 * 
 * Manages the synchronization itself. The get() function waits until there is
 * data, and returns it. Only if the finished-flag is set, will it return null.
 * That is a signal that there will never be any more stuff in the queue.
 * 
 * TODO: The notyet handling is not very effective, we get too many of those
 * even with adding to random position. maybe some sort of sorted map of the
 * notyet times, hosts, etc.
 * 
 * @author heikki
 */
public class CrawlQueue {

  private static Logger logger = Logger
      .getLogger("com.indexdata.masterkey.localindices.crawl");
  private Vector<PageRequest> q = new Vector<PageRequest>();
  private Set<String> seen = new HashSet<String>();
  private Map<String, Long> notYet = new HashMap<String, Long>();
  private boolean finished = false;
  private int underwork = 0; // counts elements that are under work

  private Random rnd = new Random();

  private synchronized void _add(PageRequest pg, boolean checkSeen,
      boolean randomposition) {
    if (!checkSeen || !seen.contains(pg.url.toString())) {
      seen.add(pg.url.toString());
      // For some strange reason, using strings in the set is
      // much faster.
      if (!randomposition || q.size() < 10) {
	// logger.log(Level.TRACE, "q+ Added link " + pg.url.toString() +
	// " ( d=" + pg.depth + ")");
	q.add(pg);
      } else {
	int n = q.size();
	int p = (n / 2) + rnd.nextInt(n / 2);
	// logger.log(Level.TRACE, "q+ Added link at " + p + "/" + n + " " +
	// pg.url.toString() +" ( d=" + pg.depth + ")");
	q.add(p, pg);
      }
    } else {
      // logger.log(Level.TRACE, "q+ " + pg.url.toString() + " already seen");
    }
  }

  private synchronized PageRequest _pop() {
    if (q.isEmpty()) {
      return null;
    }
    PageRequest pg = q.remove(0);
    incrementUnderWork();
    // logger.log(Level.TRACE, "q- " + pg.url.toString());
    return pg;
  }

  // Increment the underwork counter
  // to indicate that an element has been popped from the queue, but is still
  // under work, so we can not be finished - it may produce more work later
  private synchronized void incrementUnderWork() {
    underwork++;
  }

  // Decrement the underwork counter
  // Any thread that gets a value from the queue, must decrement the
  // underwork counter when done with it!
  public synchronized void decrementUnderWork() {
    underwork--;
  }

  public synchronized int getUnderWork() {
    return underwork;
  }

  public synchronized boolean alldone() {
    return isEmpty() && (getUnderWork() == 0);
  }

  public void add(SiteRequest site) {
    PageRequest pg = new PageRequest(site.url);
    pg.sitereq = site;
    pg.depth = 1;
    _add(pg, true, false);
  }

  public void add(PageRequest basepage, URL url) {
    PageRequest pg = new PageRequest();
    pg.sitereq = basepage.sitereq;
    pg.depth = basepage.depth + 1;
    pg.url = url;
    if (pg.sitereq == null || pg.depth <= pg.sitereq.maxdepth) {
      _add(pg, true, true);
    }
  }

  public PageRequest get() {
    String prevHost = "";
    while (!finished) {
      PageRequest pg = _pop();

      if (pg != null) {
	String host = pg.url.getHost();
	Long ny = notYet.get(host);
	Long now = System.currentTimeMillis();
	if (ny == null || ny < now) {
	  notYet.put(host, now + 2 * 60 * 1000); // 2 minutes
	  // When the page is processed, we put a better time there!
	  // most likely shorter. This stays in case of I/O errors etc
	  return pg;
	}
	if (prevHost.equals(host)) {
	  _add(pg, false, true); // same host, put in random place
	} else {
	  _add(pg, false, false); // otherwise put at end
	}
	prevHost = host;
	// logger.log(Level.TRACE, "not yet! " + host + " " + (ny - now) +
	// " " + pg.url.toString());
	decrementUnderWork(); // that one is not under work, it went back
      }
      try {
	Thread.sleep(500);
	// just to make sure that other threads can run, if our queue
	// only contains notyet entries, as can happen near the end of
	// a job.
	// also, when the queue is empty, but not finished, we try
	// to sleep and wait for new data to come in.
	// Should not be too long, as we may loop through a long list
	// of notyet-links on the first round(s).
      } catch (InterruptedException ex) {
	logger.log(Level.TRACE, "Sleep interrupted, never mind");
      }
    }
    return null; // in case we are finished
  }

  public void setNotYet(PageRequest pg, int milliSeconds) {
    String host = pg.url.getHost();
    Long now = System.currentTimeMillis();
    notYet.put(host, now + milliSeconds);
  }

  public boolean hasFinished() {
    return finished;
  }

  public void setFinished() {
    finished = true;
  }

  public boolean isEmpty() {
    return q.isEmpty();
  }

  public int size() {
    return q.size();
  }

  public int numSeen() {
    return seen.size();
  }
}
