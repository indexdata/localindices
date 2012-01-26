package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

public class ConsoleRecordStorage implements RecordStorage {

  private boolean overrideMode;
  private String database = null;
  StorageJobLogger logger; 
  
  
  private void message(Object msg) {
    System.out.println(msg);
    if (logger != null) {
      	logger.info(msg.toString());
    }
  }
  
  @Override
  public void begin() throws IOException {
    message("Begin");
  }

  @Override
  public void commit() throws IOException {
    message("Commit");
  }

  @Override
  public void rollback() throws IOException {
    message("Rollback");
  }

  @Override
  public void purge() throws IOException {
    message("Purge");
  }

  @Override
  public void setOverwriteMode(boolean mode) {
    message("setOverrideMode");
    overrideMode = mode;
  }

  @Override
  public boolean getOverwriteMode() {
    return overrideMode;
  }

  @Override
  public OutputStream getOutputStream() {
    throw new RuntimeException("OutputStream interface not supported");
  }

  @Override
  public void databaseStart(String database, Map<String, String> properties) {
    this.database = database;
    message("Start database: " + database);
  }

  @Override
  public void databaseEnd() {
    message("database End: " + database);
    database = null;
  }

  @Override
  public void add(Map<String, Collection<Serializable>> keyValues) {
    message("Adding as Map.");
    add(new RecordImpl(keyValues));
  }

  @Override
  public void add(Record record) {
    message("Add " + record.toString());
  }

  @Override
  public Record get(String id) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void delete(String id) {
    message("Delete Record{id=" + id + "}");
  }

  @Override
  public void setLogger(StorageJobLogger logger) {
    this.logger = logger;
  }
}