package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;

import com.indexdata.masterkey.localindices.harvest.storage.DatabaseContenthandler;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorageProxy;
import com.indexdata.masterkey.localindices.harvest.storage.StatusNotImplemented;
import com.indexdata.masterkey.localindices.harvest.storage.StorageStatus;

public class NoCommitPurgeStorageProxy extends RecordStorageProxy {

  private StorageJobLogger logger; 
  
  public NoCommitPurgeStorageProxy(RecordStorage storage) {
    setTarget(storage);
  }

  public void purge(boolean commit) {
    if (logger != null)
      logger.info("purge is being ignored.");
    if (commit)
      commit();
  }

  public void commit() {
    if (logger != null)
      logger.info("commit is being ignored.");
  }

  @Override
  public void setLogger(StorageJobLogger logger) {
    this.logger = logger; 
  }

  @Override
  public StorageStatus getStatus() throws StatusNotImplemented {
    return getTarget().getStatus();
  }

  @Override
  public DatabaseContenthandler getContentHandler() {
    return getTarget().getContentHandler();
  }

  @Override
  public void shutdown(HarvestStatus status) throws IOException {
    getTarget().shutdown(status);
  }

  @Override
  public void setBatchLimit(int limt) {
    getTarget().setBatchLimit(limt);
  }
  
  
}
