/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.job;

import java.io.File;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.indexdata.masterkey.localindices.client.XmlMarcClient;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.cache.DiskCache;
import com.indexdata.masterkey.localindices.harvest.storage.StorageException;

/**
 * This class handles HTTP download of file(s), and bulk transformation
 *
 * @author Dennis Schafroth
 *
 */
public class BulkRecordHarvestJob extends AbstractRecordHarvestJob {

  @SuppressWarnings("unused")
  private List<URL> urls = new ArrayList<URL>();
  private XmlBulkResource resource;
  //private RecordStorage transformationStorage;
  private Proxy proxy;
  private String errors;
  private HarvestStatus initialStatus;
  

  public BulkRecordHarvestJob(XmlBulkResource resource, Proxy proxy) {
    this.proxy = proxy;
    this.resource = resource;
    this.resource.setMessage(null);
    initialStatus = HarvestStatus.valueOf(resource.getCurrentStatus());
    setStatus(initialStatus);
    setLogger((new FileStorageJobLogger(getClass(), resource)));
  }

  public int getNumber(String value, int defaultValue) {
    int number;
    if (value != null && !"".equals(value)) {
      try {
        number = Integer.parseInt(value);
        if (number < 0) {
          number = defaultValue;
        }
        return number;
      } catch (NumberFormatException nfe) {
        logger.warn("Unable to parse number: " + value);
      }
    }
    return defaultValue;
  }

  @Override
  public String getMessage() {
    return error;
  }

  @Override
  public void run() {
    String subject = "";
    String msg = "";
    try {
      resource.setMessage(null);
      resource.setAmountHarvested(null);
      getStorage().setLogger(logger);
      logger.info("Batch limit "+resource.getStorageBatchLimit());
      if (resource.getStorageBatchLimit() != null) {
        logger.info("Type of the record storage "+getStorage().getClass().getCanonicalName());
        getStorage().setBatchLimit(resource.getStorageBatchLimit());
      }
      // This is different from old behavior. All insert is now done in one commit.
      getStorage().begin();
      getStorage().databaseStart(resource.getId().toString(), null);
      DiskCache dc = new DiskCache(resource.getId());
      if (resource.isCacheEnabled()) 
	dc.init();
      if (resource.getOverwrite()) {
        getStorage().purge(false);
        if (resource.isCacheEnabled() && !resource.isDiskRun())
          dc.empty();
      }
      setStatus(HarvestStatus.RUNNING);
      if (!resource.isDiskRun())
        downloadList(resource.getUrl().split("[, \n]"), false, dc);
      else {
        downloadList(dc.list(), true, dc);
      }
      subject = "Completed: ";
      msg = "OK";
      if (getStatus() == HarvestStatus.RUNNING)
	setStatus(HarvestStatus.OK);
      if (getStatus() == HarvestStatus.WARN || getStatus() == HarvestStatus.ERROR) {
	subject = "Harvest status: " + getStatus().toString() ;
	msg = getHarvestable().getMessage();
	logError(subject, msg);
      }

      if ( getStatus() == HarvestStatus.OK || getStatus() == HarvestStatus.WARN || 
	 ((getStatus() == HarvestStatus.ERROR || getStatus() == HarvestStatus.KILLED) && getHarvestable().getAllowErrors())) 
      {
	if ((getStatus() == HarvestStatus.ERROR || getStatus() == HarvestStatus.KILLED)) {
	  logger.info("Commiting records on error/kill status due to checked 'Allow Errors'.");
	}
	transformationStorage.databaseEnd();
        commit();
        setStatus(HarvestStatus.FINISHED);
      }
      else {
        transformationStorage.databaseEnd();
        transformationStorage.rollback();
      }
    } catch (Exception e) {
      setStatus(HarvestStatus.ERROR, e.getMessage());
      logger.error("Failed. " + e.getMessage(), e);
      try {
	if (e instanceof StorageException) {
	  logger.info("No attempt to rollback due to StorageException");
	  subject = "Storage Error";
	  msg = e.getMessage();
	}
	else {
	  msg = "Failed to complete job, rolling back...";
	  subject = "Error";
	  getStorage().rollback();
	}
      } catch (Exception ioe) {
	msg += " Roll-back failed: " + ioe.getMessage();  
        logger.error(msg);
      }
      subject = "Harvest failed";
      msg = e.getMessage();
      logError(subject, msg);
    } finally {
      mailMessage(subject, msg);
      shutdown();
    }
  }

  private void downloadList(String[] list, boolean diskRun, DiskCache dc) throws Exception {
    Date lastDate = null;
    if (resource.getAllowCondReq()) {
      //conditonal request are enabled, manual override takes precedence and
      //gets cleaned-up
      lastDate = resource.getFromDate();
      resource.setFromDate(null);
      //when no manual override, use the last completion date
      if (lastDate == null) {
        lastDate = initialStatus == HarvestStatus.ERROR 
          || initialStatus == HarvestStatus.WARN 
          ? null 
          : resource.getLastHarvestFinished();
      }
    }
    XmlMarcClient client = new XmlMarcClient(resource, this, proxy, logger, dc, lastDate);
    for (String item : list) {
      try {
        int noErrors = 0;
        if (!diskRun)
          noErrors = client.download(new URL(item));
        else
          noErrors = client.download(new File(item));
        if (noErrors > 0) {
          setStatus(HarvestStatus.WARN, client.getErrors());
        }
        
      } catch (Exception e) {
	if (isKillSent()) {
	  logger.info("Job killed. Stopping.");
	  return;
	}
	if (resource.getAllowErrors()) {
          if (errors == null) {
            errors = "Failed to harvest: ";
          }
          errors += item + ": " + e.getMessage();
          setStatus(HarvestStatus.WARN, errors);
        } else {
          throw e;
        }
      }
    }
  }

  @Override
  public Harvestable getHarvestable() {
    return resource;
  }
}
