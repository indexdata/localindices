/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import java.io.OutputStream;

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
  public void kill();

  /**
   * Get latest harvest status.
   * 
   * @return current status
   */
  public HarvestStatus getStatus();

  /**
   * Sets the storage for the the harvested data.
   * 
   * @param storage
   *          for the harvest
   */
  public void setStorage(RecordStorage storage);

  /**
   * Returns storage currently used for harvested data.
   * 
   * @return current storage
   */
  public RecordStorage getStorage();

  /**
   * Inform the harvestesting job the the files harvest were received.
   */
  public void finishReceived();

  /**
   * Get last harvesting error.
   * 
   * @return
   */
  public String getMessage();

  public boolean isUpdated();

  public void clearUpdated();

  public OutputStream getOutputStream();

}
