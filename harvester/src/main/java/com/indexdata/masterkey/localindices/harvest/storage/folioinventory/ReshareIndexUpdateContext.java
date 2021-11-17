package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.StorageException;

import java.util.Map;

public class ReshareIndexUpdateContext extends FolioUpdateContext {

  private static final String RESHARE_INDEX_PATH = "reshareIndexPath";
  public String reshareIndexPath;

  public ReshareIndexUpdateContext(Harvestable harvestable, StorageJobLogger logger) throws StorageException {
    super(harvestable, logger);
    Storage storage = harvestable.getStorage();
    setStorageConfig(storage);
  }

  @Override
  public void moduleDatabaseStart(String database, Map<String, String> properties) {

  }

  @Override
  public void moduleDatabaseEnd() {

  }

  @Override
  protected void setFolioModuleConfigs() {
    reshareIndexPath = getConfig(RESHARE_INDEX_PATH);
  }

  @Override
  public String getStoragePath() {
    return reshareIndexPath;
  }
}
