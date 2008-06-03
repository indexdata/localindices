/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.scheduler;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The scheduler thread runs the actuall JobScheduler. It controls 
 * the sleep timeout and waits for kill signals. The instantiated object is
 * placed in the appplication context and can be retrieved across the application.
 * 
 * @author jakub
 */
public class SchedulerThread implements Runnable {
    private boolean keepRunning;
    private JobScheduler scheduler;
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.harvester");

    public SchedulerThread() {
        scheduler = new JobScheduler();
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
}

