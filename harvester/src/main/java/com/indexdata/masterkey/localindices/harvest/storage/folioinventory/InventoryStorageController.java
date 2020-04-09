package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.harvest.job.FileStorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.DatabaseContenthandler;
import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordJSON;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.StatusNotImplemented;
import com.indexdata.masterkey.localindices.harvest.storage.StorageException;
import com.indexdata.masterkey.localindices.harvest.storage.StorageStatus;

/**
 * The logic for initializing storage, loggers, execution and performance
 * statistics, retrieving job configurations, and then receiving and iterating
 * Inventory record sets coming in from the transformation pipeline and
 * forwarding each record to be created/updated/deleted by {@link InventoryRecordUpdater}
 *
 * @author kurt
 */
public class InventoryStorageController implements RecordStorage {
  protected String authToken;
  protected CloseableHttpClient client;
  protected StorageJobLogger logger;
  protected Harvestable harvestable;

  protected Map<String, String> databaseProperties;
  protected StorageStatus storageStatus;
  protected String folioAddress;

  protected static final String FOLIO_AUTH_PATH = "folioAuthPath";
  protected static final String FOLIO_TENANT = "folioTenant";
  protected static final String FOLIO_USERNAME = "folioUsername";
  protected static final String FOLIO_PASSWORD = "folioPassword";
  protected static final String INSTANCE_STORAGE_PATH = "instanceStoragePath";
  protected static final String HOLDINGS_STORAGE_PATH = "holdingsStoragePath";
  protected static final String ITEM_STORAGE_PATH = "itemStoragePath";

  protected final Map<String,String> locationsToInstitutionsMap = new HashMap<String,String>();

  protected RecordUpdateCounts counters;
  protected HourlyPerformanceStats timingsEntireRecord;


  @Override
  public void setHarvestable(Harvestable harvestable) {
    this.harvestable = harvestable;
    init();
  }

  private void init() {
    try {
      Storage storage = null;
      if(harvestable != null) {
        storage = harvestable.getStorage();
      }
      this.folioAddress = storage.getUrl();
      logger = new FileStorageJobLogger(InventoryStorageController.class, harvestable);
      counters = new RecordUpdateCounts();
      timingsEntireRecord = new HourlyPerformanceStats(logger);
      logger.info("Initialized InventoryRecordStorage");
    } catch(Exception e) {
      throw new RuntimeException("Unable to init: " + e.getLocalizedMessage(), e);
    }
  }

  @Override
  public void databaseStart(String database, Map<String, String> properties) {
    logger.info("Database started [" + database + "]" + (properties != null ? ", with properties " + properties : " (no properties defined) "));
    this.databaseProperties = properties;
    try {
      client = HttpClients.createDefault();
      String folioAuthPath = getConfigurationValue(FOLIO_AUTH_PATH);
      String folioUsername = getConfigurationValue(FOLIO_USERNAME);
      String folioPassword = getConfigurationValue(FOLIO_PASSWORD);
      String folioTenant   = getConfigurationValue(FOLIO_TENANT, "diku");
      if (folioUsername != null && folioPassword != null && folioTenant != null && folioAuthPath != null) {
        authToken = getAuthtoken(client, folioAddress, folioAuthPath, folioUsername, folioPassword, folioTenant);
      } else {
        logger.warn("Init Inventory storage: Missing one or more pieces of FOLIO authentication information. "
                + "Will attempt to continue without for tenant [" + folioTenant + "]."
                + "This would only work for a FOLIO instance with no authentication enabled.");
      }
      try {
        cacheLocationsMap();
      } catch (IOException ioe) {
        logger.error("Failed to initialize storage for the harvest job, could not load locations from Inventory: " + ioe.getMessage());
        throw new StorageException(ioe.getMessage());
      }  catch (ParseException pe) {
        logger.error("Failed to initialize storage for the harvest job, could not parse Inventory locations response: " + pe.getMessage());
        throw new StorageException(pe.getMessage());
      }
      storageStatus = new InventoryStorageStatus(folioAddress + folioAuthPath, authToken);
    } catch (StorageException se) {
      throw se;
    } catch (IOException ioe) {
      throw new StorageException("IO exception setting up access to FOLIO ", ioe);
    }
  }

  @SuppressWarnings("unchecked")
  private String getAuthtoken(CloseableHttpClient client,
                              String folioAddress,
                              String folioAuthPath,
                              String username,
                              String password,
                              String tenant)
      throws UnsupportedEncodingException, IOException, StorageException {
    HttpPost httpPost = new HttpPost(folioAddress + folioAuthPath);
    JSONObject loginJson = new JSONObject();
    loginJson.put("username", username);
    loginJson.put("password", password);
    StringEntity entity = new StringEntity(loginJson.toJSONString());
    httpPost.setEntity(entity);
    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("Content-type", "application/json");
    httpPost.setHeader("X-Okapi-Tenant", tenant);
    CloseableHttpResponse response = client.execute(httpPost);
    if(response.getStatusLine().getStatusCode() != 201) {
      throw new StorageException(String.format("Got bad response obtaining authtoken: %s, %s",
          response.getStatusLine().getStatusCode(), EntityUtils.toString(response.getEntity())));
    }
    return response.getFirstHeader("X-Okapi-Token").getValue();
  }

  /**
   * Retrieve a mapping from locations to institutions from Inventory storage
   * Used for holdings/items deletion logic.
   * @throws IOException
   * @throws ParseException
   */
  private void cacheLocationsMap() throws IOException, ParseException {
    String url = String.format("%s", folioAddress + "locations?limit=9999");
    HttpGet httpGet = new HttpGet(url);
    setHeaders(httpGet, "application/json");
    CloseableHttpResponse response = client.execute(httpGet);
    if(! Arrays.asList(200, 404).contains(response.getStatusLine().getStatusCode())) {
      throw new IOException(String.format("Got error retrieving locations",
           EntityUtils.toString(response.getEntity())));
    }
    JSONObject jsonResponse;
    JSONParser parser = new JSONParser();
    String responseString = EntityUtils.toString(response.getEntity());
    jsonResponse = (JSONObject) parser.parse(responseString);
    JSONArray locationsJson = (JSONArray) (jsonResponse.get("locations"));
    if (locationsJson != null) {
      Iterator<?> locationsIterator = locationsJson.iterator();
      while (locationsIterator.hasNext()) {
        JSONObject location = (JSONObject) locationsIterator.next();
        locationsToInstitutionsMap.put((String)location.get("id"), (String)location.get("institutionId"));
      }
      logger.info("Initialized a map of " + locationsJson.size() + " FOLIO locations to institutions.");
    } else {
      throw new StorageException("Failed to retrieve any locations from Inventory, found no 'locations' in response");
    }
  }

  @Override
  public void add(Record recordJson) {
    if (recordJson.isCollection()) {
      logger.log(Level.TRACE, "Inventory Storage received add signal with a collection payload.");
      Collection<Record> subrecords = recordJson.getSubRecords();
      if (subrecords.size()==1) {
        for (Record subRecord : subrecords) {
          logger.log(Level.TRACE, "Iterating subrecords of a RecordJSON of one subrecord");
          subRecord.setOriginalContent(recordJson.getOriginalContent());
          InventoryRecordUpdater recordStorageHandler = new InventoryRecordUpdater(this);
          recordStorageHandler.addInventory((RecordJSON) subRecord);
        }
      } else {
        if (harvestable.isStoreOriginal()) {
          logger.warn("Store original content selected for this job, "
                  + "but storage layer received "
                  + "result with multiple metadata records and original content "
                  + "cannot be stored in that scenario.");
        }
        for (Record subRecord : subrecords) {
          logger.log(Level.TRACE, "Iterating multiple subrecords of RecordJSON");
          InventoryRecordUpdater recordStorageHandler = new InventoryRecordUpdater(this);
          recordStorageHandler.addInventory((RecordJSON) subRecord);
        }
      }
    } else {
      logger.log(Level.TRACE, "Inventory Storage received add signal with a single record payload.");
      InventoryRecordUpdater recordStorageHandler = new InventoryRecordUpdater(this);
      recordStorageHandler.addInventory((RecordJSON) recordJson);
    }
  }
  @Override
  public void delete(Record record) {
    InventoryRecordUpdater updater = new InventoryRecordUpdater(this);
    updater.delete((RecordJSON) record);
  }

  @Override
  public void databaseEnd() {
    String instancesMessage = "Instances processed/loaded/deletions/failed: " + counters.instancesProcessed + "/" + counters.instancesLoaded + "/" + counters.instanceDeletions + "/" + counters.instancesFailed + ". ";
    String holdingsRecordsMessage = "Holdings records processed/loaded/deleted/failed: " + counters.holdingsRecordsProcessed + "/" + counters.holdingsRecordsLoaded + "/" + counters.holdingsRecordsDeleted + "/" + counters.holdingsRecordsFailed + ". ";
    String itemsMessage = "Items processed/loaded/deleted/failed: " + counters.itemsProcessed + "/" + counters.itemsLoaded + "/" + counters.itemsDeleted + "/" + counters.itemsFailed + ".";
    String sourceRecordsMessage = "Source records processed/loaded/deleted/failed: " + counters.sourceRecordsProcessed + "/" + counters.sourceRecordsLoaded + "/" + counters.sourceRecordsDeleted + "/" + counters.sourceRecordsFailed + ".";

    logger.log((counters.instancesFailed>0 ? Level.WARN : Level.INFO), instancesMessage);
    logger.log((counters.holdingsRecordsFailed>0 ? Level.WARN : Level.INFO), holdingsRecordsMessage);
    logger.log((counters.itemsFailed>0 ? Level.WARN : Level.INFO), itemsMessage);
    logger.log((counters.sourceRecordsFailed>0 ? Level.WARN : Level.INFO), sourceRecordsMessage);

    for (String key : counters.instanceExceptionCounts.keySet()) {
      logger.info(String.format("%d Instance records failed with %s", counters.instanceExceptionCounts.get(key),key));
    }
    timingsEntireRecord.writeLog();
    harvestable.setMessage(instancesMessage + " " + holdingsRecordsMessage + " " + itemsMessage + " " + sourceRecordsMessage);
  }

  @Override
  public StorageStatus getStatus() throws StatusNotImplemented {
    return this.storageStatus;
  }

  /**
   * Retrieves setting by name from the free-form JSON config column of Harvestable
   * @param key
   * @return
   */
  protected String getConfigurationValue(String key) {
    String value = null;
    if (harvestable != null) {
      String configurationsJsonString = harvestable.getJson();
      if (configurationsJsonString != null && configurationsJsonString.length()>0) {
      try {
        JSONParser parser = new JSONParser();
        JSONObject configurations = (JSONObject) parser.parse(configurationsJsonString);
        value = (String) configurations.get(key);
      } catch (ParseException pe) {
        logger.warn("Could not parse JSON configuration from harvestable.json [" + configurationsJsonString + "]");
      }
      if (value == null) {
          logger.warn("Did not find value for key [" + key + "] in 'configurations JSON': " + configurationsJsonString);
        }
      } else {
        logger.warn("Cannot find value for key [" + key + "] because harvestable.json is empty");
      }
    } else {
      logger.warn("Cannot find value for key [" + key + "] from 'harvestable.json' because harvestable is null");
    }
    return value;
  }

  protected String getConfigurationValue (String key, String defaultValue) {
    String value = getConfigurationValue (key);
    return value != null ? value : defaultValue;
  }

  private void setHeaders (HttpRequestBase request, String accept) {
    request.setHeader("Accept", accept);
    request.setHeader("Content-type", "application/json");
    request.setHeader("X-Okapi-Token", this.authToken);
    request.setHeader("X-Okapi-Tenant", this.getConfigurationValue(InventoryStorageController.FOLIO_TENANT));
  }

  // Unsupported interface methods
  @Override
  public void begin() throws IOException {
    logger.debug("Transaction begin request recieved (not supported for Inventory updates)");
  }

  @Override
  public void commit() throws IOException {
    logger.debug("Commit request recieved (not supported for Inventory updates)");
  }

  @Override
  public void rollback() throws IOException {
    logger.debug("Rollback request recieved  (not supported for Inventory updates)");
  }

  @Override
  public void purge(boolean commit) throws IOException {
    logger.debug("Purge request recieved  (not supported for Inventory updates)");
  }

  @Override
  public void setOverwriteMode(boolean mode) {
    throw new UnsupportedOperationException("set overwrite mode Not supported.");
  }

  @Override
  public boolean getOverwriteMode() {
    throw new UnsupportedOperationException("get overwrite mode Not supported.");
  }

  @Override
  public void add(Map<String, Collection<Serializable>> keyValues) {
    throw new UnsupportedOperationException("Adding record by key-values collection not supported.");
  }

  @Override
  public Record get(String id) {
    throw new UnsupportedOperationException("get by id not supported.");
  }

  @Override
  public void delete(String id) {
    throw new UnsupportedOperationException("delete by id not supported.");
  }

  @Override
  public void setLogger(StorageJobLogger logger) {
    throw new UnsupportedOperationException("set logger not supported.");
  }

  @Override
  public DatabaseContenthandler getContentHandler() {
    throw new UnsupportedOperationException("retrieving content handler not supported.");
  }

  @Override
  public void shutdown() throws IOException {
    throw new UnsupportedOperationException("shutdown not supported.");
  }

  @Override
  public void setBatchLimit(int limit) {
    throw new UnsupportedOperationException("set batch limit not supported.");
  }


}
