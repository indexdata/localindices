package com.indexdata.masterkey.localindices.harvest.storage.folio;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.StorageException;
import org.apache.log4j.Level;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Map;

public class ShareIndexUpdateContext extends FolioUpdateContext {

  private static final String SHARED_INDEX_PATH = "sharedIndexPath";
  public String sharedIndexPath;
  public String sourceId;
  public HourlyPerformanceStats timingsCreatingRecord;
  public HourlyPerformanceStats timingsTransformingRecord;
  public HourlyPerformanceStats timingsIndexEntry;

  public ShareIndexUpdateContext(Harvestable harvestable, StorageJobLogger logger) throws StorageException {
    super(harvestable, logger);
    Storage storage = harvestable.getStorage();
    setStorageConfig(storage);
    sourceId = getJsonStringProperty(getJsonConfig(), "sourceId");
    timingsIndexEntry = new HourlyPerformanceStats("Storing records", logger);
    timingsCreatingRecord = new HourlyPerformanceStats("Creating DOM for incoming record", logger);
    timingsCreatingRecord.setLogLevelForIntervals(Level.DEBUG);
    timingsTransformingRecord = new HourlyPerformanceStats("Transforming incoming record before storing", logger);
    timingsTransformingRecord.setLogLevelForIntervals(Level.DEBUG);
  }

  private JSONObject getJsonConfig () {
    JSONObject json = new JSONObject();
    try {
      if (harvestable.getJson() == null || harvestable.getJson().isEmpty()) {
        return json;
      } else {
        return (JSONObject) (new JSONParser()).parse(harvestable.getJson());
      }
    } catch (ParseException | ClassCastException pe) {
      logger.error("Could not parse the harvestable's json config as a JSON object: " + pe.getMessage());
      return json;
    }
  }

  private static String getJsonStringProperty (JSONObject json, String name) {
    if (json.containsKey(name)) {
      return json.get(name).toString();
    } else {
      return null;
    }
  }

  @Override
  public void moduleDatabaseStart(String database, Map<String, String> properties) {

  }

  @Override
  public void moduleDatabaseEnd() {

  }

  @Override
  protected void setFolioModuleConfigs() {
    sharedIndexPath = getConfig(SHARED_INDEX_PATH);
  }

  @Override
  public String getStoragePath() {
    return sharedIndexPath;
  }
}
