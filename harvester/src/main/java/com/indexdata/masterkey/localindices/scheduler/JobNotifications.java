package com.indexdata.masterkey.localindices.scheduler;

import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;

public interface JobNotifications {
  /**
   * Notify that the job is finished 
   * @param job
   */
  void finished(RecordHarvestJob job);
  /**
   * Notify that the job has aborted 
   * @param job
   */
  void aborted(RecordHarvestJob job);
  /**
   * Request persisting of state 
   * @param job
   */
  void persist(RecordHarvestJob job);
}
