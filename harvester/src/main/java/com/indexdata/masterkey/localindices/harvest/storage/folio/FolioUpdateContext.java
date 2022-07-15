package com.indexdata.masterkey.localindices.harvest.storage.folio;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.StorageException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.time.Instant;
import java.util.Map;

public abstract class FolioUpdateContext {

  public Harvestable harvestable;
  protected static final String FOLIO_TENANT = "folioTenant";
  protected static final String FOLIO_AUTH_PATH = "folioAuthPath";
  protected static final String FOLIO_USERNAME = "folioUsername";
  protected static final String FOLIO_PASSWORD = "folioPassword";
  protected static final String FOLIO_AUTH_SKIP = "folioAuthSkip";

  public String folioAddress;
  public JSONObject storageConfig;
  public String folioTenant;
  public String folioAuthPath;
  public String folioUsername;
  public String folioPassword;
  public boolean folioAuthSkip = false;
  public String authToken;
  protected boolean isXmlBulkJob;
  protected Instant xmlBulkRecordFilteringDate;
  public FailedRecordsController failedRecordsController;

  public CloseableHttpClient folioClient = null;
  protected boolean statusWritten = false;
  public FolioStorageStatus storageStatus;

  public StorageJobLogger logger;

  public FolioUpdateContext (Harvestable harvestable, StorageJobLogger logger) throws StorageException {
    this.logger = logger;
    this.harvestable = harvestable;
    storageStatus = new FolioStorageStatus();
  }

  public abstract void moduleDatabaseStart(String database, Map<String, String> properties);

  public abstract void moduleDatabaseEnd();

  protected abstract void setFolioModuleConfigs();

  public abstract String getStoragePath();

  protected String getRequiredConfig(String key) throws StorageException {
    String val = (String) storageConfig.get(key);
    if (val == null || val.length()==0) {
      logger.error("Missing required FOLIO storage configuration for ["+ key + "].");
      throw new StorageException("Missing mandatory configuration value [" + key + "]. Cannot perform harvest job");
    } else {
      return val;
    }
  }

  public String getConfig(String key) {
    return (String) storageConfig.get(key);
  }

  public String getConfig(String key, String defaultValue) {
    return (storageConfig.get(key) != null ? (String) storageConfig.get(key) : "false" );
  }

  protected void setFolioConfigs() {
    folioTenant = getRequiredConfig(FOLIO_TENANT);
    folioAuthSkip = getConfig(FOLIO_AUTH_SKIP, "false").equalsIgnoreCase("true");
    if ( !folioAuthSkip )
    {
      folioAuthPath = getRequiredConfig(FOLIO_AUTH_PATH);
      folioUsername = getRequiredConfig(FOLIO_USERNAME);
      folioPassword = getRequiredConfig(FOLIO_PASSWORD);
    }
    isXmlBulkJob = (harvestable instanceof XmlBulkResource );
    if (isXmlBulkJob && !harvestable.getOverwrite()) {
      if (harvestable.getFromDate() != null) {
        xmlBulkRecordFilteringDate = harvestable.getFromDate().toInstant();
      } else if (harvestable.getLastHarvestFinished() != null) {
        xmlBulkRecordFilteringDate = harvestable.getLastHarvestFinished().toInstant();
      }
      if (xmlBulkRecordFilteringDate != null) {
        logger.info(
                "Filtering records by date, excluding records with an update date before " + xmlBulkRecordFilteringDate );
      }
    }
  }

  protected JSONObject getStorageConfigJson (Storage storage) {
    String configurationsJsonString = storage.getJson();
    if (configurationsJsonString != null && configurationsJsonString.length()>0) {
      try {
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(configurationsJsonString);
      } catch ( ParseException pe) {
        String error = "Could not parse JSON configuration from harvestable.json [" + configurationsJsonString + "]";
        logger.error(error + pe.getMessage());
        throw new StorageException (error,pe);
      }
    } else {
      String error = "Cannot find required configuration for Inventory storage (looking in STORAGE.JSON). Cannot perform job.";
      logger.error(error);
      throw new StorageException(error);
    }
  }

  protected void setStorageConfig(Storage storage) {
    folioAddress = storage.getUrl();
    storageConfig = getStorageConfigJson(storage);
    setFolioConfigs();
    setFolioModuleConfigs();
  }

  public void setClient (CloseableHttpClient folioClient) {
    this.folioClient = folioClient;
  }

  public void setAuthToken (String token) {
    this.authToken = token;
  }

  protected void setHeaders (HttpRequestBase request, String accept) {
    request.setHeader("Accept", accept);
    request.setHeader("Content-type", "application/json");
    request.setHeader("X-Okapi-Token", authToken);
    request.setHeader("X-Okapi-Tenant", folioTenant);
  }

  /**
   * If harvestable is an XML bulk job and 'overwrite' is not checked, and if the job has a 'fromDate' or
   * alternatively a 'lastHarvestFinished' date, then date filtering applies
   * @return true if date filtering applies for this job.
   */
  public boolean xmlBulkRecordFilteringApplies() {
    return xmlBulkRecordFilteringDate != null;
  }

  public Instant getBulkRecordFilteringInstant() {
    return xmlBulkRecordFilteringDate;
  }

}
