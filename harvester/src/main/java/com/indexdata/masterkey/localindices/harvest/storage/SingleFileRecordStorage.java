/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

/**
 * Simple, single file storage.
 * 
 * @author jakub
 */
public class SingleFileRecordStorage extends SingleFileStorage implements RecordStorage {

  public SingleFileRecordStorage(String outFileName) {
    super(outFileName);
  }

  public SingleFileRecordStorage(Harvestable harvestable) {
    super(harvestable);
  }

  @SuppressWarnings("unused")
  private String outFileName;
  private OutputStream fos;

  public void setOverwriteMode(boolean mode) {
    if (mode) {
      try {
	begin();
	purge(true);
      } catch (IOException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
      }
    }
  }

  public boolean getOverwriteMode() {
    return false;
  }

  @Override
  public void databaseStart(String database, Map<String, String> properties) {
  }

  @Override
  public void databaseEnd() {
  }

  @Override
  public void add(Map<String, Collection<Serializable>> keyValues) {
  }

  @Override
  public void add(Record record) {
    try {
      fos.write(record.toString().getBytes());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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

  @Override
  public DatabaseContenthandler getContentHandler() {
    return new Pz2SolrRecordContentHandler(this, null);
  }

  @Override
  public void shutdown() {
    try {
      fos.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  
}
