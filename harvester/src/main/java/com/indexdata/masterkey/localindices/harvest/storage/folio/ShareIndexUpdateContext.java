package com.indexdata.masterkey.localindices.harvest.storage.folio;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.StorageException;
import org.apache.log4j.Level;

import java.util.Map;

public class ShareIndexUpdateContext extends FolioUpdateContext {

  private static final String RESHARE_INDEX_PATH = "reshareIndexPath";
  public String reshareIndexPath;
  public HourlyPerformanceStats timingsCreatingRecord;
  public HourlyPerformanceStats timingsTransformingRecord;
  public HourlyPerformanceStats timingsIndexEntry;

  public ShareIndexUpdateContext(Harvestable harvestable, StorageJobLogger logger) throws StorageException {
    super(harvestable, logger);
    Storage storage = harvestable.getStorage();
    setStorageConfig(storage);
    timingsIndexEntry = new HourlyPerformanceStats("Storing Inventory records", logger);
    timingsCreatingRecord = new HourlyPerformanceStats("Creating DOM for incoming record", logger);
    timingsCreatingRecord.setLogLevelForIntervals(Level.DEBUG);
    timingsTransformingRecord = new HourlyPerformanceStats("Transforming incoming record before storing", logger);
    timingsTransformingRecord.setLogLevelForIntervals(Level.DEBUG);
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
