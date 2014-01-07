package com.indexdata.masterkey.localindices.harvest.job;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.scheduler.JobNotifications;

public class DummyJobNotifications implements JobNotifications {
  public Logger logger = Logger.getLogger(this.getClass());
  @Override
  public void finished(RecordHarvestJob job) {
    logger.info("Job#(" + job + ") finished");
  }

  @Override
  public void aborted(RecordHarvestJob job) {
    logger.info("Job#(" + job + ") aborted");
  }

  @Override
  public void persist(RecordHarvestJob job) {
    logger.info("Job#(" + job + ") requested persisting");
  }

}
