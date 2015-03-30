/*
 * Copyright (c) 1995-2009, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.harvest.storage.AbstractTransformationRecordStorageProxy;
import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.StatusNotImplemented;
import com.indexdata.masterkey.localindices.harvest.storage.StorageStatus;
import com.indexdata.masterkey.localindices.harvest.storage.ThreadedTransformationRecordStorageProxy;
import com.indexdata.masterkey.localindices.harvest.storage.TransformationRecordStorageProxy;
import com.indexdata.masterkey.localindices.notification.Notification;
import com.indexdata.masterkey.localindices.notification.NotificationException;
import com.indexdata.masterkey.localindices.notification.Sender;
import com.indexdata.masterkey.localindices.notification.SenderFactory;
import com.indexdata.masterkey.localindices.notification.SimpleNotification;
import com.indexdata.utils.TextUtils;

/**
 * Specifies the simplest common behavior of all HarvestJobs that otherwise
 * would have to be re-implemented if every concrete job implementation.
 * 
 * @author Dennis
 */
public abstract class AbstractRecordHarvestJob implements RecordHarvestJob {
  private RecordStorage storage;
  protected StorageJobLogger logger;
  protected String error;
  boolean debug = false;
  boolean useParallel = false;
  AbstractTransformationRecordStorageProxy transformationStorage;
  private boolean updated;
  private HarvestStatus runStatus;
  private HarvestStatus jobStatus;
  private boolean die;
  private Thread jobThread;

  @Override
  public void setStorage(RecordStorage storage) {
    // Invalidate transformation storage proxy
    this.transformationStorage = null;
    this.storage = storage;
  }

  @Override
  public synchronized RecordStorage getStorage() {
    if (transformationStorage == null && storage != null) {
      Transformation transformation = getHarvestable().getTransformation();
      List<TransformationStep> steps = null;
      Boolean parallel = false;
      if (transformation != null) {
        steps = transformation.getSteps();
        parallel = transformation.getParallel();
      }
      try {
        if (new Boolean(true).equals(parallel))
          transformationStorage = new ThreadedTransformationRecordStorageProxy(
              storage, steps, this);
        else
          transformationStorage = new TransformationRecordStorageProxy(storage,
              steps, this);

      } catch (TransformerConfigurationException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return transformationStorage;
  }

  @Override
  public abstract String getMessage();

  @Override
  public StorageJobLogger getLogger() {
    return logger;
  }

  @Override
  public void setLogger(StorageJobLogger logger) {
    this.logger = logger;
  }

  protected void commit() throws IOException {
    RecordStorage storage = getStorage();
    storage.commit();
    Harvestable resource = getHarvestable();
    resource.setLastHarvestFinished(new Date());
    try {
      StorageStatus storageStatus = storage.getStatus();
      if (storageStatus != null) {
        resource.setAmountHarvested(storageStatus.getAdds());
        logger.info("Committed " + storageStatus.getAdds() + " adds, "
            + storageStatus.getDeletes() + " deletes. "
            + storageStatus.getTotalRecords()
            + " in total (pending warming of index).");
      }
    } catch (StatusNotImplemented exception) {
      logger.warn("Failed to get Storage Status.");
    }
    markForUpdate();
  }

  protected void mailMessage(String subject, String message) {
    Sender sender = SenderFactory.getSender();
    if (sender == null) {
      logger.warn("Notification sender not configured,"
        + " notification will not be sent");
      return;
    }
    String status = getStatus().toString();
    Harvestable harvestable = getHarvestable();
    HarvestStatus mailLevel = harvestable.getMailAddress() != null
      ? HarvestStatus.valueOf(harvestable.getMailLevel())
      : null;
    if (shouldSend(mailLevel, getStatus())) {
      //prepend msg with link to the job log
      StringBuilder buffer = new StringBuilder();
      if (sender.getAdminUrl() != null && !sender.getAdminUrl().isEmpty()) {
        String jobLink = 
          TextUtils.joinPath(sender.getAdminUrl(), "jobs", "log.xhtml")
          + "?resourceId="+ getHarvestable().getId() + "#bottom";
        buffer.append("For details see: ").append(jobLink).append("\n")
          .append("\n");
      } else {
        buffer.append("(please configure 'harvester.admin.url' setting in "
          + "the Settings tab to include a link to the job log in this e-mail)\n\n");
      }
      if (message != null) buffer.append(message);
      //transformation errors
      while (transformationStorage != null
          && !transformationStorage.getErrors().isEmpty()) {
        try {
          Object obj = transformationStorage.getErrors().take();
          if (obj instanceof Record) {
            buffer.append("Failed to convert: ").append(obj.toString()).append("\n");
          }
          if (obj instanceof String) {
            buffer.append("Error: ").append((Record) obj);
          }
        } catch (InterruptedException ie) {
          buffer.append("Failed to extract error message from Error Queue");
        }
      }

      Notification msg = new SimpleNotification(status, harvestable.getName()
          + "(" + harvestable.getId() + "): " + subject, buffer.toString());

      try {
        String recipients = harvestable.getMailAddress();
        if (recipients != null && !recipients.isEmpty())
          sender.send(recipients, msg, getMessageContentType()); 
        else if (sender.getDefaultRecipients() != null 
          && !sender.getDefaultRecipients().isEmpty())
          sender.send(msg, getMessageContentType()); 
        else
          logger.warn("No default or custom recipients specified,"
            + " notification will not be sent");
      } catch (NotificationException ne) {
        String mailError = "Failed to send notification: " + ne.getMessage();
        logger.error(mailError);
        logger.debug("Cause: ", ne);
      }
    }
  }
  
  protected String getMessageContentType () {
    return "text/plain; charset=UTF-8";
  }

  protected boolean shouldSend(HarvestStatus mailLevel, HarvestStatus status) {
    if (mailLevel == null || status == null) return false;
    return mailLevel.ordinal() <= status.ordinal();
  }

  protected void logError(String logSubject, String message) {
    setStatus(HarvestStatus.ERROR, message);
    getHarvestable().setMessage(message);
    logger.error(logSubject + ": " + message);
  }

  protected void shutdown() {
    getHarvestable().setDiskRun(false);
    markForUpdate();
    try {
      if (getStorage() != null)
        getStorage().shutdown();
    } catch (IOException ioe) {
      logger.warn("Storage shutdown exception: " + ioe.getMessage());
    } finally {
      transformationStorage = null;
      logger.close();
    }
  }

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
   * @param Set
   *          the job ending status
   * @param error
   *          An optional message to be displayed
   */
  @Override
  public void setStatus(HarvestStatus status, String msg) {
    setStatus(status);
    getHarvestable().setMessage(msg);
  }

  protected void markForUpdate() {
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
      Logger.getLogger(this.getClass()).warn(
          "No job thread to interrupt on kill. Slower shutdown");
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
  public final synchronized void finishReceived() {
    runStatus = jobStatus;
    Harvestable harvestable = getHarvestable();
    harvestable.setLastHarvestFinished(new Date());
  }

  @Override
  public final boolean isUpdated() {
    return updated;
  }

  @Override
  public final void clearUpdated() {
    updated = false;
  }

  public Thread getJobThread() {
    return jobThread;
  }

  public void setJobThread(Thread thread) {
    jobThread = thread;
  }

  @Override
  public abstract void run();

  @Override
  public abstract Harvestable getHarvestable();
}
