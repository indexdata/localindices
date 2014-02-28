/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;

/**
 * This interface a runnable harvest job controlled by the JobInstance
 * 
 * @author jakub
 */
public interface HarvestJob extends Runnable {

  /**
   * Stop the current job and rollback the current harvest, deleting files
   * received so far. Does not touch the older harvests.
   */
  void kill();

  /**
   * Sets the storage for the the harvested data.
   * 
   * @param storage
   *          for the harvest
   */
  void setStorage(RecordStorage storage);

  /**
   * Returns storage currently used for harvested data.
   * 
   * @return current storage
   */
  RecordStorage getStorage();

  /**
   * Returns the information about the job 
   * 
   * @return database entity for the Job
   */
  Harvestable getHarvestable();

  /**
   * Inform the harvesting job the the files harvest were received.
   */
  void finishReceived();

  /**
   * Get last harvesting error.
   * 
   * @return
   */
  String getMessage();

  boolean isUpdated();

  void clearUpdated();

  boolean isKillSent();

    void setJobThread(Thread thread);
  
  /**
   * Get latest harvest status.
   * 
   * @return current status
   */
  HarvestStatus getStatus();
  /**
   * Set harvest Status
   * 
   * @param error
   */
  void setStatus(HarvestStatus error);
  /**
   * Set harvest status and error message
   * 
   * @param error
   * @param message
   */
  void setStatus(HarvestStatus error, String message);
}
