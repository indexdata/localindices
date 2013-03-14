/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.log4j.Level;
import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.client.HarvestConnectorClient;
import com.indexdata.masterkey.localindices.entity.HarvestConnectorResource;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.SplitTransformationChainRecordStorageProxy;
import com.indexdata.masterkey.localindices.harvest.storage.ThreadedTransformationRecordStorageProxy;
import com.indexdata.masterkey.localindices.harvest.storage.TransformationRecordStorageProxy;
import com.indexdata.xml.filter.SplitContentHandler;

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
  private RecordStorage transformationStorage;
  private Thread jobThread = null;
  private boolean useParallel = true;
  
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
      templates = lookupTransformationTemplates(resource.getTransformation());
      setStatus(HarvestStatus.RUNNING);
      RecordStorage storage = getStorage(); 
      storage.setOverwriteMode(resource.getOverwrite());
      storage.begin();
      storage.databaseStart(resource.getId().toString(), null);
      HarvestConnectorClient client = new HarvestConnectorClient(resource, proxy);
      client.setHarvestJob(this);
      // The client will build it's urls 
      client.download(null);
      storage.databaseEnd();
      storage.commit();
      setStatus(HarvestStatus.FINISHED);
    } catch (Exception e) {
      setStatus(HarvestStatus.ERROR);
      error = e.getMessage();
      resource.setMessage(e.getMessage());
      logger.log(Level.ERROR, "Harvest failed.", e);
    }
  }

  protected RecordStorage setupTransformation(RecordStorage storage) {
    if (resource.getTransformation() != null && resource.getTransformation().getSteps().size() > 0) {
      XMLReader xmlReader;
      try {
	xmlReader = createTransformChain(true);
	SplitContentHandler splitHandler = new SplitContentHandler(new RecordStorageConsumer(getStorage()), 1, 1);
	xmlReader.setContentHandler(splitHandler);
	return new SplitTransformationChainRecordStorageProxy(storage, xmlReader, null);
      } catch (Exception e) {
	e.printStackTrace();
	logger.error(e.getMessage());
      }
    }
    logger.warn("No Transformation Proxy configured.");
    return storage;
  }

  @Override
  public OutputStream getOutputStream() {
    logger.debug("Using deprecated stream interface");
    streamTransformationStorage = setupTransformation(getStorage());
    return streamTransformationStorage.getOutputStream();
  }

  @Override
  protected Harvestable getHarvestable() {
    return resource;
  }
 
  @Override
  public synchronized RecordStorage getStorage() {
    if (transformationStorage == null) {
      try {
	if (useParallel)
	  transformationStorage = new ThreadedTransformationRecordStorageProxy(super.getStorage(), templates,
	    logger);
	else
	  transformationStorage = new TransformationRecordStorageProxy(super.getStorage(), templates,
		    logger);
	  
      } catch (TransformerConfigurationException e) {
	e.printStackTrace();
      } catch (IOException e) {
	e.printStackTrace();
      }
    }
    return transformationStorage;
  }

  public Thread getJobThread() {
    return jobThread;
  }

  public void setJobThread(Thread jobThread) {
    this.jobThread = jobThread;
  }
}
