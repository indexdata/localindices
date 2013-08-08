/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.job;

import java.io.OutputStream;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;

import com.indexdata.masterkey.localindices.client.HarvestConnectorClient;
import com.indexdata.masterkey.localindices.entity.HarvestConnectorResource;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;

/**
 * This class handles a Harvest Connector Job
 * 
 * @author jakub
 */
public class ConnectorHarvestJob extends AbstractRecordHarvestJob {
  private String error;
  @SuppressWarnings("unused")
  private List<URL> urls = new ArrayList<URL>();
  private HarvestConnectorResource resource;
  private Proxy proxy;
  private RecordStorage streamTransformationStorage;
  private Thread jobThread = null;
  public ConnectorHarvestJob(HarvestConnectorResource resource, Proxy proxy) {
    this.proxy = proxy;
    this.resource = resource;
    setStatus(HarvestStatus.valueOf(resource.getCurrentStatus()));
    this.resource.setMessage(null);
    logger = new FileStorageJobLogger(this.getClass(), resource);
  }

  public void setStorage(HarvestStorage storage) {
    if (storage instanceof RecordStorage) 
      super.setStorage((RecordStorage) storage); 
  }

  public String getMessage() {
    return error;
  }

  @Override
  public synchronized void kill() {
    super.kill();
    // Requires that the job instances configures the thread
    if (jobThread != null) {
      jobThread.interrupt();
    }
  }

  public void run() {
    try {
      setStatus(HarvestStatus.RUNNING);
      RecordStorage storage = getStorage(); 
      storage.setOverwriteMode(resource.getOverwrite());
      storage.begin();
      storage.databaseStart(resource.getId().toString(), null);
      if (resource.getOverwrite())
          storage.purge(false);
      HarvestConnectorClient client = new HarvestConnectorClient(resource, proxy);
      client.setHarvestJob(this);
      // The client will build it's URLs 
      client.download(null);
      if (getStatus() == HarvestStatus.RUNNING)
	setStatus(HarvestStatus.OK);
      storage.databaseEnd();
      commit();
      setStatus(HarvestStatus.FINISHED);
    } catch (Exception e) {
      setStatus(HarvestStatus.ERROR);
      error = e.getMessage();
      resource.setMessage(e.getMessage());
      logger.log(Level.ERROR, "Harvest failed.", e);
    }
  }

  protected RecordStorage setupTransformation(RecordStorage storage) {
    splitSize = 1;
    splitDepth =  1;
    return super.setupTransformation(storage);
  }

  @Override
  public OutputStream getOutputStream() {
    logger.debug("Using deprecated stream interface");
    streamTransformationStorage = setupTransformation(getStorage());
    //if (streamStorage.isClosed)))
    	
    return streamTransformationStorage.getOutputStream();
  }

  @Override
  protected Harvestable getHarvestable() {
    return resource;
  }
 
  public Thread getJobThread() {
    return jobThread;
  }

  public void setJobThread(Thread jobThread) {
    this.jobThread = jobThread;
  }
}
