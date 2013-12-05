/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.entity.FileStorageEntity;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

/**
 * Simple console storage that prints out the harvested data.
 * 
 * @author Jakub
 */
public class FileStorage implements RecordStorage {
  private boolean overwriteMode = false;
  File current; 
  Harvestable harvestable;
  OutputStream out;
  private String database;
  private boolean committed;
  private long deletes;
  private long adds;
  private StorageJobLogger logger = null;
  public FileStorage() {
    
  }
  
  public void setHarvestable(Harvestable harvestable) {
    this.harvestable = harvestable; 
  }
  
  public void begin() throws IOException {
    Storage storage = harvestable.getStorage();
    if (storage instanceof FileStorageEntity) {
      FileStorageEntity fileStorage = (FileStorageEntity) storage;
      String url = fileStorage.getIndexingUrl();
      if (url.startsWith("file://")) {
	url = url.substring("file://".length());
      }
      try {
	current = new File(url);
	out = new FileOutputStream(current, true);
      } catch (NullPointerException npe) {
	throw new IOException(npe);
      }
    }
    logger.debug("--- Storage write begin ---");
  }

  public void commit() throws IOException {
    logger.debug("--- Storage write commited ---");
    committed = true;
    out.close();
    out = null;
    current = null;
  }

  public void rollback() throws IOException {
    logger.debug("--- Storage write rolled back, last write discarded ---");
  }

  public void purge(boolean commit) throws IOException {
    logger.debug("--- Storage write purged, all previous write discarded ---");
    if (out != null)
      out.close();
    out = new FileOutputStream(current, false);

    if (commit) {
      commit();
//      out = new FileOutputStream(current, false);
    }
  }

  public OutputStream getOutputStream() {
    if (out == null) {
      logger.error("Error: getOutputStream before begin. File is not open: " + (current != null? current.getAbsolutePath() : "Path not set!"));
    }
    return out;
  }

  public void setOverwriteMode(boolean mode) {
    logger.debug("--- OverwriteMode set to " + mode);
    overwriteMode = mode;
  }

  public boolean getOverwriteMode() {
    return overwriteMode;
  }

  @Override
  public void databaseStart(String database, Map<String, String> properties) {
    this.database = database;
    logger.debug("Database name: " + database);
  }

  @Override
  public void databaseEnd() {
    logger.debug("Database ended: " + database);
  }

  @Override
  public void add(Map<String, Collection<Serializable>> keyValues) {
    adds++;
    // TODO Serialize and write to output file
  }

  @Override
  public void add(Record record) {
    adds++;
  }

  @Override
  public Record get(String id) {
    // Won't implement. 
    return null;
  }

  @Override
  public void delete(String id) {
    // Won't implement. 
  }

  @Override
  public void setLogger(StorageJobLogger logger) {
    this.logger = logger; 
  }

  @Override
  public StorageStatus getStatus() throws StatusNotImplemented {
    return new SimpleStorageStatus(adds, deletes, committed);
  }

  @Override
  public DatabaseContenthandler getContentHandler() {
    return new Pz2SolrRecordContentHandler(this, database);
  }

  @Override
  public void shutdown() {
    logger.debug("Closing Storage");
    try {
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
      logger.debug("Exception while closing output: " + e.getMessage());
    }
  }

}
