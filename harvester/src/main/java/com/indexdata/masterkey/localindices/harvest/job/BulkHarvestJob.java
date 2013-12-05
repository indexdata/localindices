/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;

import com.indexdata.masterkey.localindices.crawl.HTMLPage;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;

/**
 * This class handles bulk HTTP download of a single file.
 * 
 * @author jakub
 */
public class BulkHarvestJob extends AbstractHarvestJob  {
  private LocalIndicesLogger logger;
  private HarvestStorage storage;
  private String error;
  @SuppressWarnings("unused")
  private List<URL> urls = new ArrayList<URL>();
  private XmlBulkResource resource;
  private Proxy proxy;

  public BulkHarvestJob(XmlBulkResource resource, Proxy proxy) {
    this.proxy = proxy;
    this.resource = resource;
    setStatus(HarvestStatus.valueOf(resource.getCurrentStatus()));
    this.resource.setMessage(null);
    logger = new FileStorageJobLogger(this.getClass(), resource);
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
      downloadList(resource.getUrl().split(" "));
      setStatus(HarvestStatus.FINISHED);
    } catch (Exception e) {
      setStatus(HarvestStatus.ERROR);
      error = e.getMessage();
      resource.setMessage(e.getMessage());
      logger.log(Level.ERROR, "Download failed.", e);
    }
  }

  private void downloadList(String[] urls) throws Exception {
    try {
      storage.begin();
      if (storage.getOverwriteMode())
	storage.purge(false);
      for (String url : urls) {
	download(new URL(url));
      }
      storage.commit();
    } catch (IOException ioe) {
      storage.rollback();
      throw new Exception("Storage write failed. ", ioe);
    }
  }

  private void download(URL url) throws Exception {
    logger.log(Level.INFO, "Starting download - " + url.toString());
    try {
      HttpURLConnection conn = null;
      if (proxy != null)
	conn = (HttpURLConnection) url.openConnection(proxy);
      else
	conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      int responseCode = conn.getResponseCode();
      int contentLenght = conn.getContentLength();
      String contentType = conn.getContentType();
      if (responseCode == 200) {
	// jump page
	if (contentType.startsWith("text/html")) {
	  logger.log(Level.INFO, "Jump page found at " + url.toString());
	  HTMLPage jp = new HTMLPage(conn.getInputStream(), url);
	  if (jp.getLinks().isEmpty()) {
	    throw new Exception("No links found on the jump page");
	  }
	  int proper = 0;
	  int dead = 0;
	  int recursive = 0;
	  for (URL link : jp.getLinks()) {
	    if (proxy != null)
	      conn = (HttpURLConnection) link.openConnection(proxy);
	    else
	      conn = (HttpURLConnection) link.openConnection();
	    conn.setRequestMethod("GET");
	    responseCode = conn.getResponseCode();
	    contentLenght = conn.getContentLength();
	    contentType = conn.getContentType();
	    if (responseCode == 200) {
	      // watch for non-marc links
	      if (contentType.startsWith("text/html")) {
		logger.log(Level.WARN, "Possible sub-link ignored at " + link.toString());
		recursive++;
		continue;
		// possibly a marc file
	      } else {
		logger.log(Level.INFO, "Found file at " + link.toString());
		store(conn.getInputStream(), contentLenght);
		proper++;
	      }
	    } else {
	      logger.log(Level.WARN, "Dead link (" + responseCode + " at " + link.toString());
	      dead++;
	      continue;
	    }
	  }
	  if (proper == 0) {
	    logger.log(Level.ERROR, "No proper links found at " + url.toString()
		+ ", trash links: " + recursive + ", dead links: " + dead);
	    throw new Exception("No MARC files found at " + url.toString());
	  }
	  // assume marc file, TODO text/plain
	} else {
	  store(conn.getInputStream(), contentLenght);
	  storage.setOverwriteMode(false);
	  return;
	}
      } else {
	throw new Exception("Http connection failed. (" + responseCode + ")");
      }
      logger.log(Level.INFO, "Finished - " + url.toString());
    } catch (IOException ioe) {
      throw new Exception("Http connection failed.", ioe);
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
      if (isKillSent()) {
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

  @Override
  public synchronized boolean isKillSent() {
    boolean die = super.isKillSent();
    if (die) {
      logger.log(Level.WARN, "Bulk harvest received kill signal.");
    }
    return die;
  }

  @Override
  public OutputStream getOutputStream() {
    throw new RuntimeException("Not Implemented");
    //return null;
  }

  @Override
  public Harvestable getHarvestable() {
    return resource;
  }
}
