package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
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
import com.indexdata.masterkey.localindices.util.MarcXMLToJson;

/**
 *
 * @author kurt
 */
public class InventoryRecordStorage implements RecordStorage {
  private String authToken;
  private CloseableHttpClient client;
  protected StorageJobLogger logger;
  protected Harvestable harvestable;

  private String database;
  private Map<String, String> databaseProperties;
  private StorageStatus storageStatus;
  private String folioAddress;

  private static final String FOLIO_AUTH_PATH = "folioAuthPath";
  private static final String FOLIO_TENANT = "folioTenant";
  private static final String FOLIO_USERNAME = "folioUsername";
  private static final String FOLIO_PASSWORD = "folioPassword";
  private static final String INSTANCE_STORAGE_PATH = "instanceStoragePath";
  private static final String HOLDINGS_STORAGE_PATH = "holdingsStoragePath";
  private static final String ITEM_STORAGE_PATH = "itemStoragePath";

  private int instancesProcessed = 0;
  private int instancesLoaded = 0;
  private int instancesFailed = 0;
  private int holdingsRecordsProcessed = 0;
  private int holdingsRecordsLoaded = 0;
  private int holdingsRecordsFailed = 0;
  private int itemsProcessed = 0;
  private int itemsLoaded = 0;
  private int itemsFailed = 0;

  public InventoryRecordStorage() {
  }

  public InventoryRecordStorage(Harvestable harvestable) {
    this.harvestable = harvestable;
  }

  public void init() {
    try {
      Storage storage = null;
      if(harvestable != null) {
        storage = harvestable.getStorage();
      }
      this.folioAddress = storage.getUrl();
      logger = new FileStorageJobLogger(InventoryRecordStorage.class, harvestable);
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
    logger.info("Database started: " + database + ", with properties " + properties);
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
      storageStatus = new InventoryStorageStatus(folioAddress + folioAuthPath, authToken);
    } catch (StorageException se) {
      throw se;
    } catch (IOException ioe) {
      throw new StorageException("IO exception setting up access to FOLIO ", ioe);
    }

  }

  private String getConfigurationValue(String key) {
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

  private String getConfigurationValue (String key, String defaultValue) {
    String value = getConfigurationValue (key);
    return value != null ? value : defaultValue;
  }

  @Override
  public void databaseEnd() {
    String instancesMessage = "Instances processed: " + instancesProcessed + ". Loaded: " + instancesLoaded + ". Failed: " + instancesFailed;
    String holdingsRecordsMessage = "Holdings records processed: " + holdingsRecordsProcessed + ". Loaded: " + holdingsRecordsLoaded + ". Failed: " + holdingsRecordsFailed;
    String itemsMessage = "Items processed: " + itemsProcessed + ". Loaded: " + itemsLoaded + ". Failed: " + itemsFailed;
    logger.log((instancesFailed>0 ? Level.WARN : Level.INFO), instancesMessage);
    logger.log((holdingsRecordsFailed>0 ? Level.WARN : Level.INFO), holdingsRecordsMessage);
    logger.log((itemsFailed>0 ? Level.WARN : Level.INFO), itemsMessage);
    harvestable.setMessage(instancesMessage + " " + holdingsRecordsMessage + " " + itemsMessage);
  }

  @Override
  public void add(Map<String, Collection<Serializable>> keyValues) {
    throw new UnsupportedOperationException("Adding record by key-values collection not supported.");
  }

  @Override
  public void add(Record recordJson) {
    if (recordJson.isCollection()) {
      Collection<Record> subrecords = recordJson.getSubRecords();
      logger.debug(this.getClass().getSimpleName() + ": incoming record is a collection with " + subrecords.size() + " sub records");
      if (recordJson.getOriginalContent() != null) {
        try {
          // Note: this log level is not electable in the admin UI at time of writing
          logger.info(this.getClass().getSimpleName() + " originalContent to store for Record with a collection of " + recordJson.getSubRecords().size() + " record(s):" +  new String(recordJson.getOriginalContent(), "UTF-8"));
          JSONObject marcJson = MarcXMLToJson.convertMarcXMLToJson(new String(recordJson.getOriginalContent(), "UTF-8"));
          logger.info(marcJson.toJSONString());
        } catch (UnsupportedEncodingException uee) { logger.error("Exception in log statement: "+ uee);}
        catch( Exception e ) { logger.error("Exception caught: " + e); }

      } else {
        logger.debug(this.getClass().getSimpleName() + ": found collection of " + recordJson.getSubRecords().size() + " record(s), no original content attached.");
      }
      for (Record subRecord : subrecords) {
        JSONObject instanceWithHoldingsAndItems = ((RecordJSON) subRecord).toJson();
        addInstanceHoldingsRecordsAndItems(instanceWithHoldingsAndItems);
      }
    } else {
      logger.debug(this.getClass().getSimpleName() + ": incoming record is a single record");
      if (recordJson.getOriginalContent() != null) {
        try {
          logger.info(this.getClass().getSimpleName() + " originalContent to store: " + new String(recordJson.getOriginalContent(), "UTF-8"));
        } catch (UnsupportedEncodingException uee) {}
      } else {
        logger.info(this.getClass().getSimpleName() + ": no original content found for single record");
      }
      JSONObject instanceWithHoldingsAndItems = ((RecordJSON)recordJson).toJson();
      addInstanceHoldingsRecordsAndItems(instanceWithHoldingsAndItems);
    }
  }

  private void addInstanceHoldingsRecordsAndItems (JSONObject instanceWithHoldingsItems) {
    try {
      //JSONObject instanceWithHoldingsItems = ((RecordJSON)recordJson).toJson();
      if (instanceWithHoldingsItems.containsKey("passthrough")) {
      /* 'passthrough' is a naming convention that the transformation pipeline can
       use for a container that holds raw elements passed through the pipeline
       to be handled by subsequent transformation steps.
       If no transformation step is configured to handle the `passthrough`
       the element might show up at this point and it should be removed since
       it's not a valid Instance property */
        instanceWithHoldingsItems.remove("passthrough");
      }
      if (instanceWithHoldingsItems.containsKey("holdingsRecords")) {
        JSONArray holdingsRecords = extractJsonArrayFromObject(instanceWithHoldingsItems, "holdingsRecords");
        JSONObject instanceResponse = addInstanceRecord(instanceWithHoldingsItems);
        ((InventoryStorageStatus) storageStatus).incrementAdd(1);
        addHoldingsRecordsAndItems(holdingsRecords, instanceResponse.get("id").toString());
      } else {
        addInstanceRecord(instanceWithHoldingsItems);
        ((InventoryStorageStatus) storageStatus).incrementAdd(1);
      }
    } catch(UnsupportedEncodingException uee) {
      ((InventoryStorageStatus) storageStatus).incrementAdd(0);
      logger.error("Encoding error when adding record: " + uee.getLocalizedMessage(), uee);
    } catch(ParseException pe) {
      ((InventoryStorageStatus) storageStatus).incrementAdd(0);
      logger.error("Failed to parse response on push of record to storage as JSON: " + pe.getLocalizedMessage());
    } catch(IOException ioe) {
      ((InventoryStorageStatus) storageStatus).incrementAdd(0);
      logger.error("IO exception when adding record: " + ioe.getLocalizedMessage());
    }
  }

  /**
   * Iterate holdings records of the instanceWithHoldingsAndItems, and items of the holdings records, POST to Inventory
   * @param holdingsRecords
   * @param instanceId
   * @throws ParseException
   * @throws UnsupportedEncodingException
   * @throws IOException
   */
  private void addHoldingsRecordsAndItems(JSONArray holdingsRecords, String instanceId)
            throws ParseException, UnsupportedEncodingException, IOException {
    Iterator holdingsrecords = holdingsRecords.iterator();
    while (holdingsrecords.hasNext()) {
      JSONObject holdingsRecord;
      Object holdingsObject = holdingsrecords.next();
      if (holdingsObject instanceof JSONObject) {
        holdingsRecord = (JSONObject) holdingsObject;
        holdingsRecord.put("instanceId", instanceId);
        if (holdingsRecord.containsKey("items")) {
          JSONArray items = extractJsonArrayFromObject(holdingsRecord, "items");
          JSONObject holdingsRecordResponse = addHoldingsRecord(holdingsRecord);
          Iterator itemsIterator = items.iterator();
          while (itemsIterator.hasNext()) {
            JSONObject item = (JSONObject) itemsIterator.next();
            item.put("holdingsRecordId", holdingsRecordResponse.get("id").toString());
            addItem(item);
          }
        } else {
          addHoldingsRecord(holdingsRecord);
        }
      } else if (holdingsObject instanceof String) {
        throw new IOException("Could not parse holdings record from JSONArray: " + holdingsObject);
      }
    }
  }

  /**
   * Creates a deep clone of a JSONArray from a JSONObject, removes the array from the source object and returns the clone
   * @param jsonObject Source object containing the array to extract
   * @param arrayName Property name of the array to extract
   * @return
   * @throws ParseException
   */
  private static JSONArray extractJsonArrayFromObject(JSONObject jsonObject, String arrayName) throws ParseException {
    JSONArray array = new JSONArray();
    if (jsonObject.containsKey(arrayName)) {
      JSONParser parser = new JSONParser();
      array = (JSONArray) parser.parse(((JSONArray) jsonObject.get(arrayName)).toJSONString());
      jsonObject.remove(arrayName);
    }
    return array;
  }

  @Override
  public Record get(String id) {
    throw new UnsupportedOperationException("get by id Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void delete(String id) {
    throw new UnsupportedOperationException("delete by id Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

  /**
   * POST instance to Inventory
   * @param instanceRecord
   * @return
   * @throws UnsupportedEncodingException
   * @throws IOException
   * @throws ParseException
   */
  private JSONObject addInstanceRecord(JSONObject instanceRecord)
      throws UnsupportedEncodingException, IOException, ParseException {
    instancesProcessed++;
    String url = folioAddress +
                 getConfigurationValue(INSTANCE_STORAGE_PATH);
    HttpEntityEnclosingRequestBase httpUpdate;
    if (url.contains("instance-storage-match")) {
      httpUpdate = new HttpPut(url);
    } else {
      if (instanceRecord.containsKey("matchKey")) {
        instanceRecord.remove("matchKey");
      }
      httpUpdate = new HttpPost(url);
    }
    StringEntity entity = new StringEntity(instanceRecord.toJSONString(),"UTF-8");
    httpUpdate.setEntity(entity);
    httpUpdate.setHeader("Accept", "application/json");
    httpUpdate.setHeader("Content-type", "application/json");
    httpUpdate.setHeader("X-Okapi-Token", authToken);
    httpUpdate.setHeader("X-Okapi-Tenant", getConfigurationValue(FOLIO_TENANT));
    CloseableHttpResponse response = client.execute(httpUpdate);
    JSONParser parser = new JSONParser();
    String responseAsString = EntityUtils.toString(response.getEntity());
    JSONObject instanceResponse = null;
    try {
      instanceResponse= (JSONObject) parser.parse(responseAsString);
    } catch (ParseException pe) {
      instanceResponse = new JSONObject();
      instanceResponse.put("wrappedErrorMessage", responseAsString);
    }
    response.close();
    if(response.getStatusLine().getStatusCode() != 201 && response.getStatusLine().getStatusCode() != 200) {
      instancesFailed++;
      logger.error(String.format("Got error %s, %s adding record: %s",
              response.getStatusLine().getStatusCode(),
              responseAsString,
              instanceRecord.toJSONString()));
      throw new IOException(String.format("Error adding record %s: %s (%s)",
              instanceRecord.get("title"),
              responseAsString,
              response.getStatusLine().getStatusCode()));
    } else {
      instancesLoaded++;
      logger.info("Status code: " + response.getStatusLine().getStatusCode() + " for POST/PUT of Instance " + instancesLoaded + ", " + instanceRecord.get("title") + " UUID: " + instanceResponse.get("id"));
    }
    return instanceResponse;
  }

  /**
   * POST holdings record to Inventory
   * @param holdingsRecord
   * @return
   * @throws UnsupportedEncodingException
   * @throws IOException
   * @throws ParseException
   */
  private JSONObject addHoldingsRecord(JSONObject holdingsRecord)
      throws UnsupportedEncodingException, IOException, ParseException {
    holdingsRecordsProcessed++;
    String url = folioAddress + getConfigurationValue(HOLDINGS_STORAGE_PATH);
    HttpPost httpPost = new HttpPost(url);
    StringEntity entity = new StringEntity(holdingsRecord.toJSONString(),"UTF-8");
    httpPost.setEntity(entity);
    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("Content-type", "application/json");
    httpPost.setHeader("X-Okapi-Token", authToken);
    httpPost.setHeader("X-Okapi-Tenant", getConfigurationValue(FOLIO_TENANT));
    CloseableHttpResponse response = client.execute(httpPost);
    JSONParser parser = new JSONParser();
    String responseAsString = EntityUtils.toString(response.getEntity());
    JSONObject holdingsRecordResponse = null;
    try {
      holdingsRecordResponse= (JSONObject) parser.parse(responseAsString);
    } catch (ParseException pe) {
      holdingsRecordResponse = new JSONObject();
      holdingsRecordResponse.put("wrappedErrorMessage", responseAsString);
    }
    response.close();
    if(response.getStatusLine().getStatusCode() != 201) {
      holdingsRecordsFailed++;
      logger.error(String.format("Got error %s, %s adding record: %s",
              response.getStatusLine().getStatusCode(),
              responseAsString,
              holdingsRecord.toJSONString()));
      throw new IOException(String.format("Error adding record %s: %s (%s)",
              holdingsRecord.get("callNumber"),
              responseAsString,
              response.getStatusLine().getStatusCode()));
    } else {
      holdingsRecordsLoaded++;
      logger.info("Status code: " + response.getStatusLine().getStatusCode() + " for POST of holdings record " + holdingsRecord.get("callNumber") + " UUID: " + holdingsRecordResponse.get("id"));
    }
    return holdingsRecordResponse;
  }

  /**
   * POST item to Inventory
   * @param item
   * @return
   * @throws UnsupportedEncodingException
   * @throws IOException
   * @throws ParseException
   */
  private JSONObject addItem(JSONObject item)
      throws UnsupportedEncodingException, IOException, ParseException {
    itemsProcessed++;
    String url = folioAddress + getConfigurationValue(ITEM_STORAGE_PATH);
    HttpPost httpPost = new HttpPost(url);
    StringEntity entity = new StringEntity(item.toJSONString(),"UTF-8");
    httpPost.setEntity(entity);
    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("Content-type", "application/json");
    httpPost.setHeader("X-Okapi-Token", authToken);
    httpPost.setHeader("X-Okapi-Tenant", getConfigurationValue(FOLIO_TENANT));
    CloseableHttpResponse response = client.execute(httpPost);
    JSONParser parser = new JSONParser();
    String responseAsString = EntityUtils.toString(response.getEntity());
    JSONObject itemResponse = null;
    try {
      itemResponse= (JSONObject) parser.parse(responseAsString);
    } catch (ParseException pe) {
      itemResponse = new JSONObject();
      itemResponse.put("wrappedErrorMessage", responseAsString);
    }
    response.close();
    if(response.getStatusLine().getStatusCode() != 201) {
      itemsFailed++;
      logger.error(String.format("Got error %s, %s adding record: %s",
              response.getStatusLine().getStatusCode(),
              responseAsString,
              item.toJSONString()));
      throw new IOException(String.format("Error adding record %s: %s (%s)",
              item.get("barcode"),
              responseAsString,
              response.getStatusLine().getStatusCode()));
    } else {
      itemsLoaded++;
      logger.info("Status code: " + response.getStatusLine().getStatusCode() + " for POST of Item " + item.get("barcode") + " UUID: " + itemResponse.get("id"));
    }
    return itemResponse;

  }

  private JSONObject getInstanceRecord(CloseableHttpClient client, String id,
      String tenant, String authToken)
      throws IOException, ParseException {
    String url = String.format("%s/instances/%s", folioAddress + getConfigurationValue(INSTANCE_STORAGE_PATH), id);
    HttpGet httpGet = new HttpGet(url);
    httpGet.setHeader("Accept", "application/json");
    httpGet.setHeader("Content-type", "application/json");
    httpGet.setHeader("X-Okapi-Token", authToken);
    httpGet.setHeader("X-Okapi-Tenant", tenant);
    CloseableHttpResponse response = client.execute(httpGet);
    if(response.getStatusLine().getStatusCode() != 200) {
      throw new IOException(String.format("Got error retrieving record record with id '%s': %s",
          id, EntityUtils.toString(response.getEntity())));
    }
    JSONObject recordJson;
    JSONParser parser = new JSONParser();
    recordJson = (JSONObject) parser.parse(EntityUtils.toString(response.getEntity()));
    return recordJson;
  }

  private void deleteInstanceRecord(CloseableHttpClient client, String id,
      String tenant, String authToken) throws IOException {
    String url = String.format("%s/instances/%s", folioAddress + getConfigurationValue(INSTANCE_STORAGE_PATH), id);
    HttpDelete httpDelete = new HttpDelete(url);
    httpDelete.setHeader("Accept", "application/json");
    httpDelete.setHeader("Content-type", "application/json");
    httpDelete.setHeader("X-Okapi-Token", authToken);
    httpDelete.setHeader("X-Okapi-Tenant", tenant);
    CloseableHttpResponse response = client.execute(httpDelete);
    if(response.getStatusLine().getStatusCode() != 204) {
      throw new IOException(String.format("Got error deleting record record with id '%s': %s",
          id, EntityUtils.toString(response.getEntity())));
    }
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

  private String getResourceTypeUUID(CloseableHttpClient client, String name) {
    return UUID.randomUUID().toString();
  }


}
