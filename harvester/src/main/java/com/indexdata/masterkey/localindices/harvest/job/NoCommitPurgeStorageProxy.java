package com.indexdata.masterkey.localindices.harvest.job;

import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorageProxy;
import com.indexdata.masterkey.localindices.harvest.storage.StatusNotImplemented;
import com.indexdata.masterkey.localindices.harvest.storage.StorageStatus;

public class NoCommitPurgeStorageProxy extends RecordStorageProxy {

  //private Logger logger = Logger.getLogger(getClass());
  private StorageJobLogger logger; 
  
  public NoCommitPurgeStorageProxy(RecordStorage storage) {
    setTarget(storage);
  }

  public void purge() {
    if (logger != null)
      logger.info("purge is being ignored.");
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

}
