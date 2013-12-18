package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

public abstract class RecordStorageProxy implements RecordStorage {
  RecordStorage storage;

  public void setTarget(RecordStorage storage) {
    this.storage = storage;
  }

  public RecordStorage getTarget() {
    return storage;
  }

  @Override
  public void begin() throws IOException {
    storage.begin();
  }

  @Override
  public void commit() throws IOException {
    storage.commit();
  }

  @Override
  public void rollback() throws IOException {
    storage.rollback();
  }

  @Override
  public void purge(boolean commit) throws IOException {
    storage.purge(commit);
  }

  @Override
  public void setOverwriteMode(boolean mode) {
    storage.setOverwriteMode(mode);
  }

  @Override
  public boolean getOverwriteMode() {
    return storage.getOverwriteMode();
  }

  @Override
  public void databaseStart(String database, Map<String, String> properties) {
    storage.databaseStart(database, properties);
  }

  @Override
  public void databaseEnd() {
    storage.databaseEnd();
  }

  @Override
  public void add(Map<String, Collection<Serializable>> keyValues) {
    add(new RecordImpl(keyValues));
  }

  @Override
  public void add(Record record) {
    storage.add(record);
  }


  @Override
  public Record get(String id) {
    return storage.get(id);
  }

  @Override
  public void delete(String id) {
    storage.delete(id);
  }
  
  @Override
  public void setHarvestable(Harvestable harvestable) {
    storage.setHarvestable(harvestable);
  }

  @Override
  public abstract void setLogger(StorageJobLogger logger);

  @Override
  public StorageStatus getStatus() throws StatusNotImplemented {
    return storage.getStatus();
  }

  @Override
  public DatabaseContenthandler getContentHandler() {
    return storage.getContentHandler();
  };

  @Override
  public void shutdown() throws IOException {
    storage.shutdown();
  }
}
