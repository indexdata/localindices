/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;

/**
 * This interface a runnable harvest job controlled by the JobInstance
 * 
 * @author jakub
 */
public interface RecordHarvestJob extends HarvestJob {

  /**
   * Stop the current job and rollback the current harvest, deleting files
   * received so far. Does not touch the older harvests.
   */
  void kill();

  /**
   * Get latest harvest status.
   * 
   * @return current status
   */
  HarvestStatus getStatus();

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
   * Inform the harvestesting job the the files harvest were received.
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
  
  StorageJobLogger getLogger();
  void setLogger(StorageJobLogger job);
}
