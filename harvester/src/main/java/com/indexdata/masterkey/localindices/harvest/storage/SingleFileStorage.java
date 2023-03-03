/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.storage;

import com.indexdata.masterkey.localindices.harvest.job.HarvestStatus;
import java.io.File;
import java.io.FileOutputStream;
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
public class SingleFileStorage implements RecordStorage {
  private String outFileName;
  private OutputStream fos;

  public SingleFileStorage(Harvestable harvestable) {
    this(harvestable.getId() + "-" + harvestable.getName());
  }

  public SingleFileStorage(String outFileName) {
    this.outFileName = outFileName;
  }

  public void begin() throws IOException {
    fos = new FileOutputStream(outFileName, true);
  }

  public void commit() throws IOException {
    fos.close();
  }

  public void rollback() throws IOException {
    fos.close();
    File f = new File(outFileName);
    f.delete();
  }

  public void purge(boolean commit) throws IOException {
    this.rollback();
    if (!commit) {
      	// TODO Warn about always commit
    }
  }

  public String getOutFileName() {
    return outFileName;
  }

  @Override
  public void setHarvestable(Harvestable harvestable) {
    // TODO Auto-generated method stub

  }

  public void setOverwriteMode(boolean mode) {
    if (mode) {
      try {
	begin();
	purge(true);
	commit();
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
  public void delete(Record record) {
    // Not implemented
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
  public void shutdown(HarvestStatus status) {
    try {
      fos.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void setBatchLimit(int limt) {
  }

}
