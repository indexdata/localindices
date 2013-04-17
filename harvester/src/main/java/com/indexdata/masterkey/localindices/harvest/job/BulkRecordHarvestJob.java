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

import com.indexdata.masterkey.localindices.client.XmlMarcClient;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;

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
      if (getStatus() == HarvestStatus.ERROR) {
	throw new Exception(error);
      }
      getStorage().setLogger(logger);
      // This is different from old behavior. All insert is now done in one commit.
      getStorage().begin();
      getStorage().databaseStart(resource.getId().toString(), null);
      if (resource.getOverwrite())
	getStorage().purge(false);
      setStatus(HarvestStatus.RUNNING);
      downloadList(resource.getUrl().split(" "));
      // Do I get here on KILLED?
      setStatus(HarvestStatus.FINISHED);
      // A bit weird, that we need to close the transformation, but in order to flush out all records in the pipeline
      transformationStorage.databaseEnd();
      transformationStorage.commit();
      //getStorage().commit();
    } catch (Exception e) {
      // Test
      e.printStackTrace();      
      try {
	getStorage().rollback();
      } catch (Exception ioe) {
	logger.warn("Roll-back failed.", ioe);
      }
      setStatus(HarvestStatus.ERROR);
      error = e.getMessage();
      resource.setMessage(e.getMessage());
      logger.error("Download failed.", e);
    } finally {
      logger.close();
    }
  }

  private void downloadList(String[] urls) throws Exception 
  {
    XmlMarcClient client = new XmlMarcClient();
    client.setHarvestJob(this);
    client.setProxy(proxy);
    client.setLogger(logger);
    client.setHarvestable(resource);
    for (String url : urls) {
      client.download(new URL(url));
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

  @Override
  public void setStatus(HarvestStatus status, String message) {
    super.setStatus(status);
    error = message;
    
  }
}
