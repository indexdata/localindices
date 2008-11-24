/* Work queue for the crawler
 * 
 *  Copyright (c) 2008, Index Data
 *  All rights reserved.
 *  See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.crawl;

import java.net.URL;
import java.util.Vector;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Work queue for the crawler
 * 
 * A fairly simple queue structure. 
 * 
 * TODO: Do not add a url that has been seen already.
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
    private Vector<URL> seen = new Vector<URL>();

    private synchronized void _add(PageRequest pg) {
        if (!seen.contains(pg.url)) {
            seen.add(pg.url);
            q.add(pg);
            logger.log(Level.TRACE, "q+ Added link " + pg.url.toString() );
        } else {
            logger.log(Level.TRACE, "q+ " + pg.url.toString() + " already seen");
        }
    }

    private synchronized PageRequest _pop() {
        if (q.isEmpty()) {
            return null;
        }
        PageRequest pg = q.remove(0);
        logger.log(Level.TRACE, "q- " + pg.url.toString() );
        return pg;
    }

    public void add(SiteRequest site) {
        PageRequest pg = new PageRequest(site.url);
        _add(pg);
    }

    public void add(PageRequest basepage, URL url) {
        PageRequest pg = new PageRequest();
        pg.sitereq = basepage.sitereq;
        pg.depth = basepage.depth + 1;
        pg.url = url;
        if ( pg.depth <= pg.sitereq.maxdepth )
            _add(pg);
    }

    public PageRequest get() {
        return _pop();
    }

    public boolean isEmpty() {
        return q.isEmpty();
    }
    
    public int numSeen() {
        return seen.size();
    }
    
}
