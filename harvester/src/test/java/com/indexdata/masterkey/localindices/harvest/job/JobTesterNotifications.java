package com.indexdata.masterkey.localindices.harvest.job;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.scheduler.JobNotifications;
import com.indexdata.masterkey.localindices.scheduler.SchedulerThread;

public class JobTesterNotifications implements JobNotifications {
  
  private Logger logger = Logger.getLogger(this.getClass());
  
  public RecordHarvestJob job;
  SchedulerThread schedulerThread; 
  public JobTesterNotifications(SchedulerThread scheduler) {
    schedulerThread = scheduler; 
  }
  
  @Override
  public void finished(RecordHarvestJob job) {
    logger.info("Job#(" + job + ") finished");
    this.job = job;
    schedulerThread.kill();
  }

  @Override
  public void aborted(RecordHarvestJob job) {
    logger.info("Job#(" + job + ") aborted");
    this.job = job; 
    schedulerThread.kill();
  }

  @Override
  public void persist(RecordHarvestJob job) {
    logger.info("Job#(" + job + ") requested persisting");
    this.job = job; 
    schedulerThread.kill();
  }

}
