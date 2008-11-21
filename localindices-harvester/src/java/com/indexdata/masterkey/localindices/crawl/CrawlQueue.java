/* Work queue for the crawler
 * 
 *  Copyright (c) 2008, Index Data
 *  All rights reserved.
 *  See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.crawl;

import java.net.URL;
import java.util.Vector;

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

     private Vector<PageRequest> q = new Vector<PageRequest>();
     
     private synchronized void _add(PageRequest pg) {
         q.add(pg);
     }
     
          private synchronized PageRequest _pop () {
         if (q.isEmpty())
             return null;
         PageRequest pg = q.remove(0);
         return pg;
     }
     
     
          
     public void add (SiteRequest site){
         PageRequest pg = new PageRequest(site.url);
         _add(pg);
     }
     
     public void add(PageRequest basepage, URL url) {
         PageRequest pg = new PageRequest();
         pg.sitereq=basepage.sitereq;
         pg.depth = basepage.depth +1;
         pg.url = url;
         _add(pg);
     }
     
     public PageRequest get() {
         return _pop();
     }
     
}
