/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.scheduler;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.dao.bean.HarvestablesDAOJPA;
import com.indexdata.masterkey.localindices.harvest.storage.ConsoleStorage;
import java.util.Collection;
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

    private boolean keepRunning;
    private static Logger logger;
    private JobScheduler scheduler;
    private HarvestableDAO dao;

    public SchedulerThread(String serviceBaseURL) {
        logger = Logger.getLogger("com.indexdata.masterkey.localindices.harvester");
        dao = new HarvestablesDAOJPA();
        //dao = new HarvestableDAOWS(serviceBaseURL, logger);
        //dao = new HarvestableDAOFake();
        scheduler = new JobScheduler(dao, new ConsoleStorage(), logger);
    }

    public void run() {
        logger.log(Level.INFO, Thread.currentThread().getName() + 
                ": SchedulerThread started.");
        keepRunning = true;
        while (keepRunning()) {
            try {
                Thread.sleep(10 * 1000);
                logger.log(Level.INFO, Thread.currentThread().getName() + 
                        ": checking and updating current job list..");
                scheduler.checkJobs();
                scheduler.updateJobs();
                scheduler.checkJobs();
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, Thread.currentThread().getName() + 
                        ": SchedulerThread was interrrupted. Exiting.");
            }
        }
        scheduler.stopAllJobs();
        logger.log(Level.INFO, Thread.currentThread().getName() + 
                ": SchedulerThread exits.");
    }

    public synchronized void kill() {
        logger.log(Level.INFO, Thread.currentThread().getName() + 
                ": SchedulerThread was kindly asked to stop.");
        keepRunning = false;
    }

    public Collection<JobInfo> getJobInfo() {
        return scheduler.getJobInfo();
    }

    public void stopJob(Long jobId) {
        scheduler.stopJob(jobId);
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
    
} // class SchedulerThread

