package com.indexdata.masterkey.localindices.harvest.storage;

import com.indexdata.masterkey.localindices.harvest.job.HarvestStatus;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

public class ConsoleRecordStorage implements RecordStorage {

  private boolean overrideMode;
  private String database = null;
  StorageJobLogger logger;
  private int added;
  private int deleted;
  boolean committed;
  Harvestable harvestable;

  private void message(Object msg) {
    System.out.println(msg);
    if (logger != null) {
      	logger.info(msg.toString());
    }
  }

  @Override
  public void begin() throws IOException {
    message("Begin");
    added = 0;
    deleted = 0;
    committed  = false;
  }

  @Override
  public void commit() throws IOException {
    message("Commit");
    committed = true;
  }

  @Override
  public void rollback() throws IOException {
    message("Rollback");
    added = 0;
    deleted = 0;
  }

  @Override
  public void purge(boolean commit) throws IOException {
    message("Purge");
    if (commit)
      	commit();
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
    added++;
  }

  @Override
  public Record get(String id) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void delete(String id) {
    message("Delete Record{id=" + id + "}");
    deleted++;
  }

  @Override
  public void delete(Record record) {
    message("Delete Record{id=" + record.getId() + "}");
    deleted++;
  }

  @Override
  public void setLogger(StorageJobLogger logger) {
    this.logger = logger;
  }

  @Override
  public StorageStatus getStatus()  {
    return new SimpleStorageStatus(added, deleted, committed);
  }

  @Override
  public DatabaseContenthandler getContentHandler() {
    return new Pz2SolrRecordContentHandler(this, database);
  }

  @Override
  public void setHarvestable(Harvestable harvestable) {
    this.harvestable = harvestable;
  }

  @Override
  public void shutdown(HarvestStatus status) throws IOException{
    message("Closing/shutdown ConsoleStorage");
  }

  @Override
  public void setBatchLimit(int limt) {
  }

}