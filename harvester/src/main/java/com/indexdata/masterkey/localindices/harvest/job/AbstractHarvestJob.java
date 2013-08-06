/*
 * Copyright (c) 1995-2009, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.job;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import java.util.Date;

/**
 * Specifies the simplest common behaviour of all HarvestJobs that otherwise would have to be
 * re-implemented if every concrete job implementation.
 *
 * @author jakub
 */
public abstract class AbstractHarvestJob implements HarvestJob {

  private boolean updated;
  private HarvestStorage storage;
  private HarvestStatus runStatus;
  private HarvestStatus jobStatus;
  private boolean die;
  private Thread jobThread;

  public void setStatus(HarvestStatus status) {
    this.runStatus = status;
    // Setting the finished flag must not be reflected in the job status.
    // This is first set on notifyFinished
    if (status != HarvestStatus.FINISHED) {
      jobStatus = status;
    }
  }

  /**
   *
   * @param Set the job ending status
   * @param error An optional message to be displayed
   */
  @Override
  public void setStatus(HarvestStatus status, String msg) {
    setStatus(status);
    getHarvestable().setMessage(msg);
  }

  protected final void markForUpdate() {
    updated = true;
  }

  public synchronized boolean isKillSent() {
    return die;
  }

  @Override
  public synchronized void kill() {
    die = true;
    if (jobThread != null) {
      jobThread.interrupt();
    } else {
      Logger.getLogger(this.getClass()).warn("No job thread to interrupt on kill. Slower shutdown");
    }
    if (runStatus == HarvestStatus.RUNNING) {
      runStatus = HarvestStatus.KILLED;
    }
  }

  @Override
  public final HarvestStatus getStatus() {
    return runStatus;
  }

  @Override
  public void setStorage(HarvestStorage storage) {
    this.storage = storage;
  }

  @Override
  public HarvestStorage getStorage() {
    return this.storage;
  }

  @Override
  public final synchronized void finishReceived() 
  {
    runStatus = jobStatus;
    Harvestable harvestable = getHarvestable(); 
    harvestable.setLastHarvestFinished(new Date());
  }

  @Override
  public abstract String getMessage();

  @Override
  public final boolean isUpdated() {
    return updated;
  }

  @Override
  public final void clearUpdated() {
    updated = false;
  }

  @Override
  public abstract void run();

  protected abstract Harvestable getHarvestable();

  public Thread getJobThread() {
    return jobThread;
  }

  public void setJobThread(Thread thread) {
    jobThread = thread;
  }
}
