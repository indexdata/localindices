/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.io.InputStream;
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
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.SplitTransformationChainRecordStorageProxy;
import com.indexdata.masterkey.localindices.harvest.storage.TransformationRecordStorageProxy;
import com.indexdata.xml.filter.SplitContentHandler;

/**
 * This class handles a Harvest Connector Job
 * 
 * @author jakub
 */
public class ConnectorHarvestJob extends AbstractRecordHarvestJob {
  private StorageJobLogger logger;
  private String error;
  @SuppressWarnings("unused")
  private List<URL> urls = new ArrayList<URL>();
  private HarvestConnectorResource resource;
  private Proxy proxy;
  private boolean die = false;
  private RecordStorage streamTransformationStorage;
  private RecordStorage transformationStorage;

  public ConnectorHarvestJob(HarvestConnectorResource resource, Proxy proxy) {
    this.proxy = proxy;
    this.resource = resource;
    setStatus(HarvestStatus.valueOf(resource.getCurrentStatus()));
    this.resource.setMessage(null);
    logger = new FileStorageJobLogger(this.getClass(), resource);
  }

  private synchronized boolean isKillSendt() {
    if (die) {
      logger.log(Level.WARN, "Bulk harvest received kill signal.");
    }
    return die;
  }

  public void setStorage(HarvestStorage storage) {
    if (storage instanceof RecordStorage) 
      super.setStorage((RecordStorage) storage); 
  }

  public String getMessage() {
    return error;
  }

  public void run() {
    try {
      setStatus(HarvestStatus.RUNNING);
      RecordStorage storage = super.getStorage(); 
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

  @SuppressWarnings("unused")
  private void store(InputStream is, int contentLenght) throws Exception {
    pipe(is, getStorage().getOutputStream(), contentLenght);
  }

  private void pipe(InputStream is, OutputStream os, int total) throws IOException {
    int blockSize = 4096;
    int copied = 0;
    int num = 0;
    int logBlockNum = 256; // how many blocks to log progress
    byte[] buf = new byte[blockSize];
    for (int len = -1; (len = is.read(buf)) != -1;) {
      os.write(buf, 0, len);
      if (isKillSendt()) {
	throw new IOException("Download interruted with a kill signal.");
      }
      copied += len;
      if (num % logBlockNum == 0) {
	logger.log(Level.INFO, "Downloaded " + copied + "/" + total + " bytes ("
	    + ((double) copied / (double) total * 100) + "%)");
      }
      num++;
    }
    logger.log(Level.INFO, "Download finishes: " + copied + "/" + total + " bytes ("
	+ ((double) copied / (double) total * 100) + "%)");
    os.flush();
  }

  public synchronized boolean isKillSent() {
    return false;
  }

  public RecordStorage setupTransformation(RecordStorage storage) {
    if (resource.getTransformation() != null && resource.getTransformation().getSteps().size() > 0) {
      XMLReader xmlReader;
      try {
	xmlReader = createTransformChain(true);
	SplitContentHandler splitHandler = new SplitContentHandler(new TransformerConsumer(), 1, 1);
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
}
