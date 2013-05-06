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
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.notification.Notification;
import com.indexdata.masterkey.localindices.notification.NotificationException;
import com.indexdata.masterkey.localindices.notification.Sender;
import com.indexdata.masterkey.localindices.notification.SenderFactory;
import com.indexdata.masterkey.localindices.notification.SimpleNotification;

/**
 * This class handles HTTP download of file(s), and bulk transformation
 * 
 * @author Dennis Schafroth
 * 
 */
public class BulkRecordHarvestJob extends AbstractRecordHarvestJob 
{
  @SuppressWarnings("unused")
  private List<URL> urls = new ArrayList<URL>();
  private XmlBulkResource resource;
  //private RecordStorage transformationStorage;
  private Proxy proxy;
  private String errors;

  public BulkRecordHarvestJob(XmlBulkResource resource, Proxy proxy) {
    this.proxy = proxy;
    this.resource = resource;
    splitDepth = getNumber(resource.getSplitAt(), splitDepth); 
    splitSize  = getNumber(resource.getSplitSize(), splitSize);
    this.resource.setMessage(null);
    setStatus(HarvestStatus.valueOf(resource.getCurrentStatus()));
    setLogger((new FileStorageJobLogger(getClass(), resource)));
  }

  private int getNumber(String value, int defaultValue) {
    int number;
    if (value != null && !"".equals(value)) {
      try {
	number = Integer.parseInt(value);
	if (number < 0)
	  number = defaultValue;
	return number;
      } catch (NumberFormatException nfe) {
	logger.warn("Unable to parse number: " + value);
      }
    }
    return defaultValue;
  }
  
  public String getMessage() {
    return error;
  }

  public void run() {
    try {

      // Don't start if we already are in error
      if (!resource.getAllowErrors() && getStatus() == HarvestStatus.ERROR) {
	  logger.error("Already in ERROR. Set Allowed Errors to run job");
	  return ; 
      }

      getStorage().setLogger(logger);
      // This is different from old behavior. All insert is now done in one commit.
      getStorage().begin();
      getStorage().databaseStart(resource.getId().toString(), null);
      if (resource.getOverwrite())
	getStorage().purge(false);
      setStatus(HarvestStatus.RUNNING);
      downloadList(resource.getUrl().split(" "));
      if (getStatus() != HarvestStatus.WARN && getStatus() != HarvestStatus.ERROR)
	setStatus(HarvestStatus.FINISHED);
      else {
	Sender sender = SenderFactory.getSender();
	String status = getStatus().toString();
	Notification msg = new SimpleNotification(status, resource.getName(), resource.getMessage());
	try {
	  sender.send(msg);
	} catch (NotificationException e1) {
	  logger.error("Failed to send notification " + resource.getMessage()) ;
	}
      }
      // A bit weird, that we need to close the transformation, but in order to flush out all records in the pipeline
      transformationStorage.databaseEnd();
      transformationStorage.commit();
      //getStorage().commit();
    } catch (Exception e) {
      // Test
      e.printStackTrace();
      logger.log(Level.ERROR, "Failed to complete job. Caught Exception" + e.getMessage() + ". Rolling back.");
      try {
	getStorage().rollback();
      } catch (Exception ioe) {
	logger.warn("Roll-back failed.", ioe);
      }
      setStatus(HarvestStatus.ERROR);
      error = e.getMessage();
      resource.setMessage(e.getMessage());
      logger.error("Download failed.", e);
      Sender sender = SenderFactory.getSender();
      String status = getStatus().toString();
      Notification msg = new SimpleNotification(status, "Download failed", e.getMessage());
      try {
	sender.send(msg);
      } catch (NotificationException e1) {
	logger.error("Failed to send notification" + e.getMessage(), e);
      }
    } finally {
      logger.close();
    }
  }

  private void downloadList(String[] urls) throws Exception 
  {
    XmlMarcClient client = new XmlMarcClient(this, resource.getAllowErrors());
    client.setHarvestJob(this);
    client.setProxy(proxy);
    client.setLogger(logger);
    client.setHarvestable(resource);
    for (String url : urls) {
      try {
	int noErrors = client.download(new URL(url));
	if (noErrors > 0) {
	  setStatus(HarvestStatus.WARN, client.getErrors());
	}
      } catch (Exception e) {
	if (resource.getAllowErrors()) {
	  if (errors == null)
	    errors = "Failed to harvest: ";
	  errors += url + ": " + e.getMessage();
	  setStatus(HarvestStatus.WARN, errors);
	}
	else {
	  throw e; 
	}
      }
    }
  }
  @Override
  public void setStorage(HarvestStorage storage) {
    if (storage instanceof RecordStorage) {
      super.setStorage((RecordStorage) storage);
    }
    else {
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
