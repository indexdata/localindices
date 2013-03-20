/*
 * Copyright (c) 1995-2009, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;

/**
 * Specifies the simplest common behaviour of all HarvestJobs that otherwise
 * would have to be re-implemented if every concrete job implementation.
 * 
 * @author jakub
 */
public abstract class AbstractHarvestJob implements HarvestJob {
  private boolean updated;
  private HarvestStorage storage;
  private HarvestStatus status;
  private boolean die;
  private Thread jobThread;

  public void setStatus(HarvestStatus status) {
    this.status = status;
  }

  public void setStatus(HarvestStatus status, String error) {
    this.status = status;
    getHarvestable().setMessage(error);
  }

  protected final void markForUpdate() {
    updated = true;
  }

  public synchronized boolean isKillSent() {
    return die;
  }

  @Override
  public synchronized void kill() {
    if (status == HarvestStatus.RUNNING) {
      status = HarvestStatus.KILLED;
    }
    die = true;
    if (jobThread != null)
      jobThread.interrupt();
  }


  @Override
  public final HarvestStatus getStatus() {
    return status;
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
  public final synchronized void finishReceived() {
    if (status != null && status.equals(HarvestStatus.FINISHED)) {
      status = HarvestStatus.OK;
    }
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

  public void setJobThread(Thread jobThread) {
    this.jobThread = jobThread;
  }


}
