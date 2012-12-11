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
 * @author jakub
 */
public class FileStorage implements RecordStorage {
  private boolean overwriteMode = false;
  File current; 
  Harvestable harvestable;
  OutputStream out;
  private String database;
  
  public FileStorage() {
    
  }
  
  public void setHarvestable(Harvestable harvestable) {
    this.harvestable = harvestable; 
  }
  
  public void begin() throws IOException {
    Storage storage = harvestable.getStorage();
    if (storage instanceof FileStorageEntity) {
      FileStorageEntity fileStorage = (FileStorageEntity) storage;
      String url = fileStorage.getUrl();
      if (url.startsWith("file://")) {
	url = url.substring("file:/".length());
      }
      try {
	current = new File(url);
	out = new FileOutputStream(current, true);
      } catch (NullPointerException npe) {
	throw new IOException(npe);
      }
    }
    System.out.println("--- Storage write begun ---");
  }

  public void commit() throws IOException {
    System.out.println("--- Storage write commited ---");
    out.close();
    out = null;
    current = null;
  }

  public void rollback() throws IOException {
    System.out.println("--- Storage write rolled back, last write discarded ---");
  }

  public void purge(boolean commit) throws IOException {
    System.out.println("--- Storage write purged, all previous write discarded ---");
    if (out != null)
      out.close();
    out = new FileOutputStream(current, false);

    if (commit) {
      commit();
      out = new FileOutputStream(current, false);
    }
  }

  public OutputStream getOutputStream() {
    if (out == null) {
      System.out.println("Error: getOutputStream before bigin. File is not open: " + current.getAbsolutePath());
    }
    return out;
  }

  public void setOverwriteMode(boolean mode) {
    System.out.println("--- OverwriteMode set to " + mode);
    overwriteMode = mode;
  }

  public boolean getOverwriteMode() {
    return overwriteMode;
  }

  @Override
  public void databaseStart(String database, Map<String, String> properties) {
    this.database = database;
    System.out.println("Database name: " + database);
  }

  @Override
  public void databaseEnd() {
    System.out.println("Database ended: " + database);
    
  }

  @Override
  public void add(Map<String, Collection<Serializable>> keyValues) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void add(Record record) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Record get(String id) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void delete(String id) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setLogger(StorageJobLogger logger) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public StorageStatus getStatus() throws StatusNotImplemented {
    // TODO Auto-generated method stub
    return null;
  }

}
