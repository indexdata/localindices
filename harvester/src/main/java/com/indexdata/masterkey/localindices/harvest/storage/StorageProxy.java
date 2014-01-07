package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;

import com.indexdata.masterkey.localindices.entity.Harvestable;

public abstract class StorageProxy implements RecordStorage {
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
  public void setHarvestable(Harvestable harvestable) {
    storage.setHarvestable(harvestable);
  }
}
