package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;

public abstract class StorageProxy implements HarvestStorage {
  HarvestStorage storage;

  public void setTarget(HarvestStorage storage) {
    this.storage = storage;
  }

  public HarvestStorage getTarget() {
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
  public void purge() throws IOException {
    storage.purge();
  }

  @Override
  public void setOverwriteMode(boolean mode) {
    storage.setOverwriteMode(mode);
  }

  @Override
  public boolean getOverwriteMode() {
    return storage.getOverwriteMode();
  }
}
