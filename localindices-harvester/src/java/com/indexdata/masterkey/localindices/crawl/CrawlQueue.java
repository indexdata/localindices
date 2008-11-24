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
import java.util.Set;
import java.util.Vector;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Work queue for the crawler
 * 
 * A fairly simple queue structure. 
 * 
 * TODO: Check if enough time has passed since trying a host, and
 * if so, push it back to the queue and sleep a second (for the case
 * that all requests in the queue are not yet due, don't want too 
 * busy a loop).
 * 
 * @author heikki
 */
public class CrawlQueue {

    private static Logger logger =
            Logger.getLogger("com.indexdata.masterkey.localindices.crawl");
    private Vector<PageRequest> q = new Vector<PageRequest>();
    private Set<URL> seen = new HashSet<URL>();
    private Map<String, Long> notYet = new HashMap<String, Long>();

    private synchronized void _add(PageRequest pg, boolean checkSeen) {
        if (!checkSeen || !seen.contains(pg.url)) {
            seen.add(pg.url);
            q.add(pg);
            logger.log(Level.TRACE, "q+ Added link " + pg.url.toString() + 
                    " ( d=" + pg.depth + ")");
        } else {
            logger.log(Level.TRACE, "q+ " + pg.url.toString() + " already seen");
        }
    }

    private synchronized PageRequest _pop() {
        if (q.isEmpty()) {
            return null;
        }
        PageRequest pg = q.remove(0);
        logger.log(Level.TRACE, "q- " + pg.url.toString());
        return pg;
    }

    public void add(SiteRequest site) {
        PageRequest pg = new PageRequest(site.url);
        pg.sitereq=site;
        pg.depth=1;
        _add(pg, true);
    }

    public void add(PageRequest basepage, URL url) {
        PageRequest pg = new PageRequest();
        pg.sitereq = basepage.sitereq;
        pg.depth = basepage.depth + 1;
        pg.url = url;
        if (pg.sitereq == null || pg.depth <= pg.sitereq.maxdepth) {
            _add(pg, true);
        }
    }

    public PageRequest get() {
        while (true) {
            PageRequest pg = _pop();
            if (pg == null) {
                return pg;
            }
            String host = pg.url.getHost();
            Long ny = notYet.get(host);
            Long now = System.currentTimeMillis();
            if (ny == null || ny < now) {
                notYet.put(host, now + 10 * 60 * 1000);  // 10 minutes
                // When the page is processed, we put a shorter time there!
                return pg;
            }
            _add(pg, false);
            //TODO: Keep previous host, and if same, add to a random 
            // position in the later half of the list. 
            //logger.log(Level.TRACE, "not yet! " + host + " " + (ny - now));
            try {
                Thread.sleep(1000); 
                // just to make sure that other threads can run, if our queue
                // only contains notyet entries, as can happen near the end of
                // a job.
            } catch (InterruptedException ex) {
                logger.log(Level.TRACE, "Sleep interrupted, never mind");
            }
        }
    }

    public void setNotYet(PageRequest pg, int milliSeconds ) {
        String host = pg.url.getHost();
        Long now = System.currentTimeMillis();
        notYet.put(host, now + milliSeconds );  
    }

    public boolean isEmpty() {
        return q.isEmpty();
    }

    public int numSeen() {
        return seen.size();
    }
}
