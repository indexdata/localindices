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

import com.indexdata.masterkey.localindices.client.HarvestConnectorClient;
import com.indexdata.masterkey.localindices.entity.HarvestConnectorResource;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;

/**
 * This class handles a Harvest Connector Job
 * 
 * @author jakub
 */
public class ConnectorHarvestJob extends AbstractRecordHarvestJob {
  @SuppressWarnings("unused")
  private List<URL> urls = new ArrayList<URL>();
  private HarvestConnectorResource resource;
  private Proxy proxy;
  private Thread jobThread = null;
  
  public ConnectorHarvestJob(HarvestConnectorResource resource, Proxy proxy) {
    this.proxy = proxy;
    this.resource = resource;
    setStatus(HarvestStatus.valueOf(resource.getCurrentStatus()));
    this.resource.setMessage(null);
    logger = new FileStorageJobLogger(this.getClass(), resource);
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
    String startResumptionToken = resource.getResumptionToken();
    try {
      resource.setMessage(null);
      resource.setAmountHarvested(null);
      setStatus(HarvestStatus.RUNNING);
      RecordStorage storage = getStorage(); 
      storage.setOverwriteMode(resource.getOverwrite());
      storage.begin();
      
      storage.databaseStart(resource.getId().toString(), null);
      if (resource.getOverwrite())
          storage.purge(false);
      HarvestConnectorClient client = new HarvestConnectorClient(resource, this, proxy, logger, null);
      // The client will build it's URLs 
      client.download(null);
      if (getStatus() == HarvestStatus.RUNNING)
	setStatus(HarvestStatus.OK);
      //if we have accumulated errors, warn
      if (!client.getErrors().isEmpty()) {
        setStatus(HarvestStatus.WARN, "Completed with problems, see logfile for details.");
      }
      storage.databaseEnd();
      commit();
      setStatus(HarvestStatus.FINISHED);
    } catch (Exception e) {
      if (isKillSent()) {
	if (resource.getAllowErrors()) 
	  try { 
	    logger.log(Level.INFO, "Harvest Job killed. Commiting partial due to Allow Errors");
	    commit();
	    return ; 
	  } catch (Exception ioe) {
	    logger.error("Failed to commit on job killed: " + ioe.getMessage());
	  }
      }
      String error = e.getMessage();
      setStatus(HarvestStatus.ERROR, error);
      resource.setResumptionToken(startResumptionToken);
      logger.log(Level.ERROR, "Harvest failed: " + error, e);
    }
    finally {
      shutdown();
    }
  }

  @SuppressWarnings("deprecation")
  protected RecordStorage setupTransformation(RecordStorage storage) {
    splitSize = 1;
    splitDepth =  1;
    return super.setupTransformation(storage);
  }

  @Override
  public Harvestable getHarvestable() {
    return resource;
  }
 
  public Thread getJobThread() {
    return jobThread;
  }

  public void setJobThread(Thread jobThread) {
    this.jobThread = jobThread;
  }

}
