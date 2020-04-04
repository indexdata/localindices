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
import com.indexdata.masterkey.localindices.harvest.storage.Pz2SolrRecordContentHandler;
import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordJSON;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.StatusNotImplemented;
import com.indexdata.masterkey.localindices.harvest.storage.StorageException;
import com.indexdata.masterkey.localindices.harvest.storage.StorageStatus;

/**
 *
 * @author kurt
 */
public class InventoryStorageController implements RecordStorage {
  protected String authToken;
  protected CloseableHttpClient client;
  protected StorageJobLogger logger;
  protected Harvestable harvestable;

  protected String database;
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

  protected final Map<String,String> locationsToInstitutionsMap = new HashMap();

  protected RecordUpdateCounts counters;
  protected ExecutionTimeStats timingsEntireRecord;


  public InventoryStorageController() {
    counters = new RecordUpdateCounts();
  }

  public void init() {
    try {
      Storage storage = null;
      if(harvestable != null) {
        storage = harvestable.getStorage();
      }
      this.folioAddress = storage.getUrl();
      logger = new FileStorageJobLogger(InventoryStorageController.class, harvestable);
      timingsEntireRecord = new ExecutionTimeStats(logger);
      logger.info("Initialized InventoryRecordStorage");
    } catch(Exception e) {
      throw new RuntimeException("Unable to init: " + e.getLocalizedMessage(), e);
    }
  }

  @Override
  public void begin() throws IOException {
    logger.info("Transaction begin request recieved");
    database = harvestable.getId().toString();
  }

  @Override
  public void commit() throws IOException {
    logger.info("Commit request recieved");
  }

  @Override
  public void rollback() throws IOException {
    logger.info("Rollback request recieved");
  }

  @Override
  public void purge(boolean commit) throws IOException {
    logger.info("Purge request recieved");
  }

  @Override
  public void setOverwriteMode(boolean mode) {
    throw new UnsupportedOperationException("set overwrite mode Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean getOverwriteMode() {
    throw new UnsupportedOperationException("get overwrite mode Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void setHarvestable(Harvestable harvestable) {
    this.harvestable = harvestable;
    init();
  }

  @Override
  public void databaseStart(String database, Map<String, String> properties) {
    logger.info("Database started: " + database + (properties != null ? ", with properties " + properties : " (no properties) "));
    this.databaseProperties = properties;
    this.database = database;
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

  /**
   * Retrieve a mapping from locations to institutions from Inventory storage
   * Used for holdings/items deletion logic.
   * @throws IOException
   * @throws ParseException
   */
  private void cacheLocationsMap() throws IOException, ParseException {
    String url = String.format("%s", folioAddress + "locations?limit=9999");
    HttpGet httpGet = new HttpGet(url);
    httpGet.setHeader("Accept", "application/json");
    httpGet.setHeader("Content-type", "application/json");
    httpGet.setHeader("X-Okapi-Token", authToken);
    httpGet.setHeader("X-Okapi-Tenant", getConfigurationValue(FOLIO_TENANT));
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
      Iterator<JSONObject> locationsIterator = locationsJson.iterator();
      while (locationsIterator.hasNext()) {
        JSONObject location = locationsIterator.next();
        locationsToInstitutionsMap.put((String)location.get("id"), (String)location.get("institutionId"));
      }
      logger.info("Initialized a map of " + locationsJson.size() + " FOLIO locations to institutions.");
    } else {
      throw new StorageException("Failed to retrieve any locations from Inventory, found no 'locations' in response");
    }
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
  public void add(Map<String, Collection<Serializable>> keyValues) {
    throw new UnsupportedOperationException("Adding record by key-values collection not supported.");
  }

  @Override
  public void add(Record recordJson) {
    if (recordJson.isCollection()) {
      Collection<Record> subrecords = recordJson.getSubRecords();
      if (subrecords.size()==1) {
        for (Record subRecord : subrecords) {
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
          InventoryRecordUpdater recordStorageHandler = new InventoryRecordUpdater(this);
          recordStorageHandler.addInventory((RecordJSON) subRecord);
        }
      }
    } else {
      InventoryRecordUpdater recordStorageHandler = new InventoryRecordUpdater(this);
      recordStorageHandler.addInventory((RecordJSON) recordJson);
    }
  }

  @Override
  public Record get(String id) {
    throw new UnsupportedOperationException("get by id Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void delete(String id) {
    throw new UnsupportedOperationException("delete by id Not supported."); //To change body of generated methods, choose Tools | Templates.
  }

  /**
   * Deletes a bib record with holdings and items from a shared inventory,
   * which doesn't mean removing the Instance and all it's holdings entirely,
   * but rather removing the identifier for the current library from the shared
   * instance as well as the holdings for the current library, while leaving the
   * master instance and the holdings of other institutions intact.
   *
   * @param id
   */
  @Override
  public void delete(Record record) {
    InventoryRecordUpdater handler = new InventoryRecordUpdater(this);
    handler.delete((RecordJSON) record);
  }


  @Override
  public void setLogger(StorageJobLogger logger) {
    throw new UnsupportedOperationException("set logger Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public StorageStatus getStatus() throws StatusNotImplemented {
    return this.storageStatus;
  }

  @Override
  public DatabaseContenthandler getContentHandler() {
    return new Pz2SolrRecordContentHandler(this, database);
  }

  @Override
  public void shutdown() throws IOException {
    throw new UnsupportedOperationException("shutdown Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void setBatchLimit(int limit) {
    throw new UnsupportedOperationException("set batch limit Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }


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









}
