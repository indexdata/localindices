/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.job;

import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;

import com.indexdata.masterkey.localindices.client.XmlMarcClient;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.cache.DiskCache;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import java.io.File;
import java.util.Date;

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
    splitDepth = getNumber(resource.getSplitAt(), splitDepth);
    splitSize = getNumber(resource.getSplitSize(), splitSize);
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

    try {
      resource.setMessage(null);
      resource.setAmountHarvested(null);

      getStorage().setLogger(logger);
      
      // This is different from old behavior. All insert is now done in one commit.
      getStorage().begin();
      getStorage().databaseStart(resource.getId().toString(), null);
      DiskCache dc = new DiskCache(resource.getId());
      if (resource.getOverwrite()) {
        getStorage().purge(false);
        if (!resource.isDiskRun()) dc.purge();
      }
      setStatus(HarvestStatus.RUNNING);
      if (!resource.isDiskRun())
        downloadList(resource.getUrl().split(" "), false, dc);
      else {
        downloadList(dc.list(), true, dc);
      }
      String subject = "Completed.";
      String msg = "";
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
      mailMessage(subject, msg);
    } catch (Exception e) {
      setStatus(HarvestStatus.ERROR);
      String message = "Failed to complete job. Caught Exception: " + e.getMessage() + ". Rolling back!";
      logger.log(Level.ERROR, message);
      // Should detect SolrExceptions and avoid roll back if we cannot communicate with it
      try {
        getStorage().rollback();
      } catch (Exception ioe) {
	message += "Roll-back failed: " + ioe.getMessage();  
        logger.error(message);
      }
      String subject = "Harvest failed"; 
      logError(subject, e.getMessage());
      mailMessage(subject, message);
    } finally {
      resource.setDiskRun(false);
      logger.close();
    }
  }

  private void downloadList(String[] list, boolean diskRun, DiskCache dc) throws Exception {
    Date lastDate = initialStatus == HarvestStatus.ERROR || initialStatus == HarvestStatus.WARN ? null : resource.getLastHarvestFinished();
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
  public void setStorage(HarvestStorage storage) {
    if (storage instanceof RecordStorage) {
      super.setStorage((RecordStorage) storage);
    } else {
      setStatus(HarvestStatus.ERROR);
      resource.setCurrentStatus("Unsupported StorageType: " + storage.getClass().getCanonicalName()
              + ". Requires RecordStorage");
    }
  }

  @Override
  protected Harvestable getHarvestable() {
    return resource;
  }
}
