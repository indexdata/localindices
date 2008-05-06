package com.indexdata.localindexes.scheduler;

import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;


import com.indexdata.localindexes.web.entity.Harvestable;
// TODO : We need a logging thing too!
import com.indexdata.localindexes.web.entity.OaiPmhResource;
//import java.util.logging.Level;

import com.indexdata.localindexes.web.service.client.ResourceConnector;
import com.indexdata.localindexes.web.service.converter.HarvestableConverter;
import com.indexdata.localindexes.web.service.converter.HarvestableRefConverter;
import com.indexdata.localindexes.web.service.converter.HarvestablesConverter;
import java.net.MalformedURLException;
import java.net.URL;
//import java.util.logging.Logger;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The SchedulerThread does the actual scheduling of harvester threads
 * Simple pseudocode:
 *    * Get a list of active jobs from the WS, and update the joblist
 *    * For each job in joblist
 *       * If time to run it (and not already running), start it
 *       * If running, poll for status, and pass on to the WS
 *    * Sleep a while
 *  
 * Missing:
 *    - use the HarvestableRef everywhere!
 *    - Some sort of logging
 *    - Poll the job list from Jakub's WS
 *    - Make use of Marc's harvesting classes
 *    - Create background threads
 *    - Get cron lines from the Harvestables
 *    - Compare current time to cron line
 * 
 * @author heikki
 */
public class SchedulerThread implements Runnable {

    private String serviceBaseURL;
    private boolean keepRunning;
    private Map<Long, JobInstance> jobs = new HashMap<Long, JobInstance>();
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.harvester");

    public SchedulerThread(String serviceBaseURL) {
        this.serviceBaseURL = serviceBaseURL;
    }

    public void run() {
        logger.log(Level.INFO, "SchedulerThread started.");
        keepRunning = true;
        while (keepRunning()) {
            try {
                mainLoop();
                Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "SchedulerThread was interrrupted. Exiting.", e);
            }
        }
        logger.log(Level.INFO, "SchedulerThread exiting.");
    }

    public synchronized void kill() {
        logger.log(Level.INFO, "SchedulerThread was kindly asked to stop.");
        keepRunning = false;
    }

    private synchronized boolean keepRunning() {
        return keepRunning;
    }

    /**
     * The "main" loop:
     * If time to poll the WS
     *   Get a new job list from the WS
     *   Update the current joblist
     * Check if status changed in any running threads
     * Check if time to start new threads
     */
    private void mainLoop() {
        Collection<HarvestableRefConverter> reflist = pollHarvestableRefList();
        updateJobs(reflist);
        checkJobs();
    } // mainLoop

    /**
     * Update the current job list (jobs) based on a polled harvestableRef list (refs)
     */
    private void updateJobs(Collection<HarvestableRefConverter> refs) {
        if (refs == null) {
            logger.log(Level.SEVERE, "harvestableRef list is null. Cannot update current job list");
        } else {
            // mark all job so we know what to remove
            for (JobInstance j : jobs.values()) {
                j.seen = false;
            }
            for (HarvestableRefConverter href : refs) {
                Long id = href.getId();
                JobInstance ji = jobs.get(id);
                // corresponding job is in the current list
                if (ji != null) {
                    // and seetings has changed
                    if (ji.getHarvestable().getLastUpdated() != href.getLastUpdated()) {
                        logger.log(Level.INFO, "Parameters changed for job " + ji + ", killing old thread.");
                        ji.killThread();
                        ji = null; // signal to create a new one
                        // should we remove it from the list?
                    }
                }
                // crate new job
                if (ji == null) {
                    Harvestable harv = retrieveFromRef(href);
                    try {
                        ji = new JobInstance(harv);
                        jobs.put(id, ji);
                        logger.log(Level.INFO, "Created a new job " + ji);
                    } catch (IllegalArgumentException ile) {
                        logger.log(Level.SEVERE, "Cannot update the current job list with given entity.", ile);
                    }
                }
                ji.seen = true;
            }

            // kill jobs with no entities in the WS
            for (Iterator<JobInstance> it = jobs.values().iterator(); it.hasNext();) {
                JobInstance ji = it.next();
                if (!ji.seen) {
                    logger.log(Level.INFO, "Job " + ji.getHarvestable().getId() +
                            " gone missing. Deleting");
                    ji.killThread();
                    it.remove();
                }
            }
        }
    } // updateJobs
    
    /**
     * Start, kill, report status of the running jobs.
     */
    private void checkJobs() {
        CronLine currentCronLine = CronLine.currentCronLine();
        for (JobInstance ji : jobs.values()) {
            if (ji.timeToRun(currentCronLine)) {
                ji.startThread();
            }
            if (ji.errorChanged()) {
                reportJobStatus(ji);
            }
        }
    }
    
    /**
     * Reports job status back to the Web Service
     * @param ji running job instance
     */
    private void reportJobStatus(JobInstance ji) {
        // this gotta report back to the WS
        logger.log(Level.INFO, "Harvesting job error has changed to: " + ji.getError());
    }

    /**
     * Retrieve list of all harvestables from the Web Service
     * @return
     */
    private Collection<HarvestableRefConverter> pollHarvestableRefList() {
        try {
            ResourceConnector<HarvestablesConverter> harvestablesConnector =
                    new ResourceConnector<HarvestablesConverter>(
                    new URL(serviceBaseURL),
                    "com.indexdata.localindexes.web.entity" +
                    ":com.indexdata.localindexes.web.service.converter");
            HarvestablesConverter hc = harvestablesConnector.get();
            return hc.getReferences();
        } catch (Exception male) {
            logger.log(Level.SEVERE, "Cannot retrieve the list of harvestables", male);
        }
        return null;
    }
    
    /**
     * Retrieve harvestable from the Web Service using it's reference (URL)
     * @param href harvestableRef entity
     * @return harvesatble entity
     */
    private Harvestable retrieveFromRef(HarvestableRefConverter href) {
        try {
            ResourceConnector<HarvestableConverter> harvestableConnector =
                    new ResourceConnector<HarvestableConverter>(
                    href.getResourceUri().toURL(),
                    "com.indexdata.localindexes.web.entity" +
                    ":com.indexdata.localindexes.web.service.converter");
            return harvestableConnector.get().getEntity();
        } catch (Exception male) {
            logger.log(Level.SEVERE, "Cannot retreve harvestable from it's ref", male);
        }
        return null;
    } // retrieveFromRef

    /**
     * PUT harvestable to the Web Service
     * @param harvestable entity to be put
     */
    private void updateHarvestable(Harvestable harvestable) {
        try {
            ResourceConnector<HarvestableConverter> harvestableConnector =
                    new ResourceConnector<HarvestableConverter>(
                    new URL(serviceBaseURL + harvestable.getId() + "/"),
                    "com.indexdata.localindexes.web.entity" +
                    ":com.indexdata.localindexes.web.service.converter");
            HarvestableConverter hc = new HarvestableConverter();
            hc.setEntity(harvestable);
            harvestableConnector.put(hc);
        } catch (Exception male) {
            logger.log(Level.SEVERE, "Cannot update harvestable", male);
        }
    } // updateJob
    
} // class SchedulerThread

