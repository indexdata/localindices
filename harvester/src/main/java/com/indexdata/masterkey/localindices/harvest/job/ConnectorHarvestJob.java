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

import org.apache.log4j.Level;

import com.indexdata.masterkey.localindices.client.HarvestConnectorClient;
import com.indexdata.masterkey.localindices.entity.HarvestConnectorResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;

/**
 * This class handles a Harvest Connector Job
 * 
 * @author jakub
 */
public class ConnectorHarvestJob extends AbstractHarvestJob  {
  private LocalIndicesLogger logger;
  private HarvestStorage storage;
  private String error;
  @SuppressWarnings("unused")
  private List<URL> urls = new ArrayList<URL>();
  private HarvestConnectorResource resource;
  private Proxy proxy;
  private boolean die = false;

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
    this.storage = storage;
  }

  public HarvestStorage getStorage() {
    return storage;
  }

  public String getMessage() {
    return error;
  }

  public void run() {
    try {
      setStatus(HarvestStatus.RUNNING);
      storage.setOverwriteMode(resource.getOverwrite());
      HarvestConnectorClient client = new HarvestConnectorClient(resource, proxy);
      // The client will build it's urls 
      client.download(null);
      setStatus(HarvestStatus.FINISHED);
    } catch (Exception e) {
      setStatus(HarvestStatus.ERROR);
      error = e.getMessage();
      resource.setMessage(e.getMessage());
      logger.log(Level.ERROR, "Harvest failed.", e);
    }
  }

  private void store(InputStream is, int contentLenght) throws Exception {
    pipe(is, storage.getOutputStream(), contentLenght);
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

  @Override
  public OutputStream getOutputStream() {
    throw new RuntimeException("Not Implemented");
    //return null;
  }
}
