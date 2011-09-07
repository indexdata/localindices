package com.indexdata.masterkey.localindices.harvest.job;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorageProxy;

public class NoCommitPurgeStorageProxy extends RecordStorageProxy {

  private Logger logger = Logger.getLogger(getClass());

  public NoCommitPurgeStorageProxy(RecordStorage storage) {
    setTarget(storage);
  }

  public void purge() {
    logger.info("purge is being ignored.");
  }

  public void commit() {
    logger.info("commit is being ignored.");
  }

}
