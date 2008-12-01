/*
 */
package com.indexdata.masterkey.localindices.crawl;

import com.indexdata.masterkey.localindices.harvest.job.WebHarvestJob;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * A crawler working thread
 * Crawls pages from the queue, until told to stop
 * If queue is empty, sleeps a few seconds and tries again, in case
 * something was added
 * @author heikki
 */
public class CrawlThread implements Runnable {

    public static final int CRAWLTHREAD_STARTING = 0;
    public static final int CRAWLTHREAD_QWAIT = 1;
    public static final int CRAWLTHREAD_IOWAIT = 2;
    public static final int CRAWLTHREAD_PROCESSING = 3;
    public static final int CRAWLTHREAD_SAVING = 4;
    public static final int CRAWLTHREAD_DONE = 5;
    private static Logger logger =
            Logger.getLogger("com.indexdata.masterkey.localindices.crawl");
    WebHarvestJob job;
    private CrawlQueue que;
    private String filterString;
    private int threadNumber;
    private int hitInterval;
    private int status = CRAWLTHREAD_STARTING;

    public CrawlThread(WebHarvestJob job, CrawlQueue que,
            String filterString, int threadNumber, int hitInterval) {
        this.job = job;
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
    //logger.log(Level.TRACE,"thread " + threadNumber + " set status to " + status );
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
                //logger.log(Level.TRACE, "url '"+urlStr+"' matched '" + mask + "'");
                return true;
            }
        }
        //logger.log(Level.TRACE, "url '"+urlStr+"' refused by filters");
        return false;
    }

    private void crawlPage(PageRequest pg) {
        URL curUrl = pg.url;
        if (job.getRobotCache().checkRobots(curUrl)) {
            HTMLPage pi = null;
            try {
                setStatus(CRAWLTHREAD_IOWAIT);
                pi = new HTMLPage(curUrl);
                setStatus(CRAWLTHREAD_PROCESSING);
                que.setNotYet(pg, hitInterval);
            } catch (IOException ex) {
                logger.log(Level.TRACE, "Thread " + threadNumber + ": I/O error in getting " +
                        curUrl.toString() + " : " + ex.getMessage());
            }
            if (pi != null && pi.getContent() != null &&
                    !pi.getContent().isEmpty()) {
                for (URL u : pi.getLinks()) {
                    if (filterLink(u, curUrl) &&
                            job.getRobotCache().checkRobots(u)) {
                        que.add(pg, u);
                    }
                }
                String xml = pi.toPazpar2Metadata();
                if (!xml.isEmpty()) {
                    try {
                        setStatus(CRAWLTHREAD_SAVING);
                        job.saveXmlFragment(xml);
                        setStatus(CRAWLTHREAD_PROCESSING);
                    } catch (IOException ex) {
                        job.setError("I/O error writing data: " +
                                ex.getMessage());
                        logger.log(Level.TRACE, xml);
                    }
                }
            } // got page
        } // robot ok
        /*
    else {
    logger.log(Level.TRACE, "Thread " + threadNumber +
    ": Robots NOT OK for  " + curUrl.toString());
    
    }
     * */

    }

    public void run() {
        setStatus(CRAWLTHREAD_STARTING);
        logger.log(Level.TRACE, "thread " + threadNumber + " starting");
        PageRequest pg;
        setStatus(CRAWLTHREAD_QWAIT);
        while ((pg = que.get()) != null) {
            setStatus(CRAWLTHREAD_PROCESSING);
            crawlPage(pg);
            setStatus(CRAWLTHREAD_QWAIT);
        }
        setStatus(CRAWLTHREAD_DONE);
        logger.log(Level.TRACE, "thread " + threadNumber + " finished");
    } // run
} // class Crawlthread
