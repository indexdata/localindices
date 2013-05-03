/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.scheduler;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * The scheduler thread runs the actuall JobScheduler. It controls the sleep
 * timeout and waits for kill signals. The instantiated object is placed in the
 * appplication context and can be retrieved across the application.
 * 
 * @author jakub
 */
public class SchedulerThread implements Runnable {
  private Logger logger = Logger.getLogger(this.getClass());
  private boolean keepRunning;
  private JobScheduler scheduler;

  public SchedulerThread(Map<String, Object> config, Properties props) {
    scheduler = new JobScheduler(config, props);
  }

  public void run() {
    logger.log(Level.INFO, "Scheduler started.");
    keepRunning = true;
    
    while (keepRunning()) {
      try {
	Thread.sleep(1000);
	//logger.log(Level.TRACE, "Checking and updating current job list..");
	scheduler.checkJobs();
	scheduler.updateJobs();
	scheduler.checkJobs();
      } catch (InterruptedException e) {
	// Just loop
      } catch (Exception e) {
	logger.log(Level.ERROR, "Scheduler failed with exception: " + e.getMessage(), e);	
	synchronized (this) {
	  keepRunning = false;
	}
      }
    }
    scheduler.stopAllJobs();
    logger.log(Level.INFO, "Scheduler exits.");
  }

  public synchronized void kill() {
    logger.log(Level.WARN, "Scheduler received kill signal. Exiting...");
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
