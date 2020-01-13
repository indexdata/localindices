package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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

  private final Map<String,String> locationsToInstitutionsMap = new HashMap();

  private int instancesProcessed = 0;
  private int instancesLoaded = 0;
  private int instancesFailed = 0;
  private final Map<String,Integer> instanceExceptionCounts = new HashMap();
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
    } else {
      throw new StorageException("Failed to retrieve any locations from Inventory, found no 'locations' in response");
    }
  }

  /**
   * Retrieves setting by name from the free-form JSON config column of Harvestable
   * @param key
   * @return
   */
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
    for (String key : instanceExceptionCounts.keySet()) {
      logger.info(String.format("%d Instance records failed with %s", instanceExceptionCounts.get(key),key));
    }
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
      JSONObject marcJson = null;
      logger.debug(this.getClass().getSimpleName() + ": incoming record is a collection with " + subrecords.size() + " sub records");
      if (recordJson.getOriginalContent() != null) {
        try {
          // Note: this log level is not electable in the admin UI at time of writing
          logger.info(this.getClass().getSimpleName() + " originalContent to store for Record with a collection of " + recordJson.getSubRecords().size() + " record(s):" +  new String(recordJson.getOriginalContent(), "UTF-8"));
          marcJson = MarcXMLToJson.convertMarcXMLToJson(new String(recordJson.getOriginalContent(), "UTF-8"));
          logger.info(marcJson.toJSONString());
        } catch (UnsupportedEncodingException uee) { logger.error("Exception in log statement: "+ uee);}
        catch( Exception e ) { logger.error("Exception caught: " + e); }

      } else {
        logger.debug(this.getClass().getSimpleName() + ": found collection of " + recordJson.getSubRecords().size() + " record(s), no original content attached.");
      }
      for (Record subRecord : subrecords) {
        JSONObject instanceWithHoldingsAndItems = ((RecordJSON) subRecord).toJson();
        JSONObject instanceResponse = addInstanceHoldingsRecordsAndItems(instanceWithHoldingsAndItems);
        try {
          addMarcRecord(marcJson, (String)instanceResponse.get("id"));
        } catch(Exception e) {
          logger.error("Failed to add marcJson: " + e);
        }
      }
    } else {
      logger.debug(this.getClass().getSimpleName() + ": incoming record is a single record");
      if (recordJson.getOriginalContent() != null) {
        try {
          logger.info(this.getClass().getSimpleName() + " originalContent to store: " + new String(recordJson.getOriginalContent(), "UTF-8"));
        } catch (UnsupportedEncodingException uee) {}
      } else {
        logger.debug(this.getClass().getSimpleName() + ": no original content found for single record");
      }
      JSONObject instanceWithHoldingsAndItems = ((RecordJSON)recordJson).toJson();
      if (instanceWithHoldingsAndItems.containsKey("title")) {
        addInstanceHoldingsRecordsAndItems(instanceWithHoldingsAndItems);
      } else {
        if (recordJson.isDeleted()) {
          logger.info("Record is deleted: [" + recordJson.getId() + "], [" + ((RecordJSON)recordJson).toJson() + "]");
        } else {
          logger.info("Inventory record storage received instance record that was not a delete but also with no 'title' property, ["+((RecordJSON)recordJson).toJson() +"] cannot create in Inventory, skipping. ");          
        }
      }
    }
  }

  private JSONObject addInstanceHoldingsRecordsAndItems (JSONObject instanceWithHoldingsItems) {
    JSONObject instanceResponse = null;
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
      JSONArray holdingsRecords = null;
      try {
        holdingsRecords = extractJsonArrayFromObject(instanceWithHoldingsItems, "holdingsRecords");
      } catch (ParseException pe) {
        logger.error("Failed to extract holdings records as JSONArray from the Instance JSON object: " + pe.getLocalizedMessage());
      }
      instanceResponse = addInstanceRecord(instanceWithHoldingsItems);
      if (instanceResponse != null && instanceResponse.get("id") != null && holdingsRecords != null && holdingsRecords.size()>0) {
        String instanceId = instanceResponse.get("id").toString();
        // delete existing holdings/items from the same institution
        // before attaching new holdings/items to the instance
        try {
          deleteExistingHoldingsAndItems(instanceId, holdingsRecords);
          addHoldingsRecordsAndItems(holdingsRecords, instanceId);
        } catch (ParseException | ClassCastException | IOException e) {
          holdingsRecordsFailed++;
        }
      }
      if (instanceResponse != null && instancesLoaded % 100 == 0) {
        logger.info("" + instancesLoaded + " instances, " + holdingsRecordsLoaded + " holdings records, and " + itemsLoaded + " items ingested");
        if (instancesFailed+holdingsRecordsFailed+itemsFailed>0) {
          logger.info("Failed: " + instancesFailed + " instances, " + holdingsRecordsFailed + " holdings records, " + itemsFailed + " items");
        }
      }
    }

    return instanceResponse;
  }

  /**
   * Iterate holdings records and items of the instanceWithHoldingsAndItems object,
   * and POST them to Inventory
   * @param holdingsRecords
   * @param instanceId
   * @throws ParseException
   * @throws UnsupportedEncodingException
   * @throws IOException
   */
  private void addHoldingsRecordsAndItems(JSONArray holdingsRecords, String instanceId)
            throws ParseException, UnsupportedEncodingException, IOException {
    if (holdingsRecords != null) {
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
    } else {
      logger.warn("Inventory record storage found empty list of holdings records in instance input");
    }
  }

  /**
   * Determines if two holdings records are assigned to locations within the same
   * institution.
   * @param holdingsRecordLeft
   * @param holdingsRecordRight
   * @return
   */
  private boolean fromSameInstitution(JSONObject holdingsRecordLeft, JSONObject holdingsRecordRight) {
    String leftLocationId = (String) holdingsRecordLeft.get("permanentLocationId");
    String rightLocationId = (String) holdingsRecordRight.get("permanentLocationId");
    if (locationsToInstitutionsMap.get(leftLocationId) == null) {
      logger.error("Could not find location [" + leftLocationId + "] in locations map " + locationsToInstitutionsMap.toString());
    }
    return (locationsToInstitutionsMap.get(leftLocationId).equals(locationsToInstitutionsMap.get(rightLocationId)));
  }

  /**
   * Wipes out existing holdings and items belonging to the institution from
   * which we are currently loading new holdings and items
   * @param instanceId
   * @throws IOException
   * @throws ParseException
   */
  private void deleteExistingHoldingsAndItems (String instanceId, JSONArray holdingsRecords) throws IOException, ParseException {
    logger.debug("Deleting holdings and items for Instance Id " + instanceId + " if coming from same institution");

    JSONObject newHoldingsRecord = null;
    try {
      newHoldingsRecord = (JSONObject) holdingsRecords.get(0);
    } catch (ClassCastException cce) {
      logger.error("Could not get holdingsRecord JSON object from " + holdingsRecords.get(0) + " " + cce.getMessage());
      throw cce;
    }
    if (newHoldingsRecord != null) {
      JSONArray existingHoldingsRecords = getHoldingsRecordsByInstanceId(instanceId);
      if (existingHoldingsRecords != null) {
        Iterator<JSONObject> existingHoldingsRecordsIterator = existingHoldingsRecords.iterator();
        while (existingHoldingsRecordsIterator.hasNext()) {
          JSONObject existingHoldingsRecord = existingHoldingsRecordsIterator.next();
          String existingHoldingsRecordId = (String) existingHoldingsRecord.get("id");
          if (fromSameInstitution(existingHoldingsRecord, newHoldingsRecord)) {
            JSONArray items = getItemsByHoldingsRecordId(existingHoldingsRecordId);
            if (items != null) {
              Iterator<JSONObject> itemsIterator = items.iterator();
              while (itemsIterator.hasNext()) {
                JSONObject item = itemsIterator.next();
                String itemId = (String) item.get("id");
                deleteItem(itemId);
              }
            }
            deleteHoldingsRecord(existingHoldingsRecordId);
          } else {
            logger.debug("holdingsRecord " + existingHoldingsRecordId + " belongs to a different institution (" + existingHoldingsRecord.get("permanentLocationId") +"), not deleting it.");
          }
        }
      } else {
        logger.info("No existing holdingsRecords found for the instance, nothing to delete.");
      }
    }
  }

  /**
   * Get holdings records for an instance
   * @param instanceId
   * @return
   * @throws IOException
   * @throws ParseException
   */
  private JSONArray getHoldingsRecordsByInstanceId(String instanceId)
      throws IOException, ParseException {
    String url = String.format("%s?query=instanceId%%3D%%3D%s", folioAddress + getConfigurationValue(HOLDINGS_STORAGE_PATH), instanceId);
    HttpGet httpGet = new HttpGet(url);
    httpGet.setHeader("Accept", "application/json");
    httpGet.setHeader("Content-type", "application/json");
    httpGet.setHeader("X-Okapi-Token", authToken);
    httpGet.setHeader("X-Okapi-Tenant", getConfigurationValue(FOLIO_TENANT));
    CloseableHttpResponse response = client.execute(httpGet);
    if(! Arrays.asList(200, 404).contains(response.getStatusLine().getStatusCode())) {
      throw new IOException(String.format("Got error retrieving holdings records for instance with id '%s': %s",
          instanceId, EntityUtils.toString(response.getEntity())));
    }
    JSONObject jsonResponse;
    JSONParser parser = new JSONParser();
    String responseString = EntityUtils.toString(response.getEntity());
    jsonResponse = (JSONObject) parser.parse(responseString);
    JSONArray holdingsRecordsJson = (JSONArray) (jsonResponse.get("holdingsRecords"));
    return holdingsRecordsJson;
  }

  /**
   * Get items for a holdings record
   * @param holdingsRecordId
   * @return
   * @throws IOException
   * @throws ParseException
   */
  private JSONArray getItemsByHoldingsRecordId(String holdingsRecordId)
      throws IOException, ParseException {
    String url = String.format("%s?query=holdingsRecordId%%3D%%3D%s", folioAddress + getConfigurationValue(ITEM_STORAGE_PATH), holdingsRecordId);
    HttpGet httpGet = new HttpGet(url);
    httpGet.setHeader("Accept", "application/json");
    httpGet.setHeader("Content-type", "application/json");
    httpGet.setHeader("X-Okapi-Token", authToken);
    httpGet.setHeader("X-Okapi-Tenant", getConfigurationValue(FOLIO_TENANT));
    CloseableHttpResponse response = client.execute(httpGet);
    if(! Arrays.asList(200, 404).contains(response.getStatusLine().getStatusCode())) {
      throw new IOException(String.format("Got error retrieving items for holdingsRecord with id '%s': %s",
          holdingsRecordId, EntityUtils.toString(response.getEntity())));
    }
    JSONObject jsonResponse;
    JSONParser parser = new JSONParser();
    jsonResponse = (JSONObject) parser.parse(EntityUtils.toString(response.getEntity()));
    JSONArray itemsJson = (JSONArray) (jsonResponse.get("items"));
    return itemsJson;
  }

  /**
   * Delete a holdings record by ID
   * @param uuid
   * @throws IOException
   */
  private void deleteHoldingsRecord (String uuid) throws IOException{
    logger.debug("Deleting holdingsRecord with ID: " + uuid);
    String url = String.format("%s/%s", folioAddress + getConfigurationValue(HOLDINGS_STORAGE_PATH), uuid);
    HttpDelete httpDelete = new HttpDelete(url);
    httpDelete.setHeader("Accept", "text/plain");
    httpDelete.setHeader("Content-type", "application/json");
    httpDelete.setHeader("X-Okapi-Token", authToken);
    httpDelete.setHeader("X-Okapi-Tenant", getConfigurationValue(FOLIO_TENANT));
    CloseableHttpResponse response = client.execute(httpDelete);
    if(response.getStatusLine().getStatusCode() != 204) {
      throw new IOException(String.format("Got error deleting record record with id '%s': %s",
          uuid, EntityUtils.toString(response.getEntity())));
    }
  }

  /**
   * Delete an item by ID
   * @param uuid
   * @throws IOException
   */
  private void deleteItem (String uuid) throws IOException {
    logger.debug("Deleting item with ID: " + uuid);
    String url = String.format("%s/%s", folioAddress + getConfigurationValue(ITEM_STORAGE_PATH), uuid);
    HttpDelete httpDelete = new HttpDelete(url);
    httpDelete.setHeader("Accept", "text/plain");
    httpDelete.setHeader("Content-type", "application/json");
    httpDelete.setHeader("X-Okapi-Token", authToken);
    httpDelete.setHeader("X-Okapi-Tenant", getConfigurationValue(FOLIO_TENANT));
    CloseableHttpResponse response = client.execute(httpDelete);
    if(response.getStatusLine().getStatusCode() != 204) {
      throw new IOException(String.format("Got error deleting record record with id '%s': %s",
          uuid, EntityUtils.toString(response.getEntity())));
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

  private JSONObject addMarcRecord(JSONObject marcJson, String instanceId) throws IOException {
    String url = folioAddress + "/marc-records"; //TODO: Add configuration value
    HttpEntityEnclosingRequestBase httpPost;
    httpPost = new HttpPost(url);

    JSONObject marcPostJson = new JSONObject();
    marcPostJson.put("instanceId", instanceId);
    marcPostJson.put("parsedMarc", marcJson);
    StringEntity entity = new StringEntity(marcPostJson.toJSONString(),"UTF-8");
    httpPost.setEntity(entity);
    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("Content-type", "application/json");
    httpPost.setHeader("X-Okapi-Token", authToken);
    httpPost.setHeader("X-Okapi-Tenant", getConfigurationValue(FOLIO_TENANT));
    CloseableHttpResponse response = client.execute(httpPost);
    JSONParser parser = new JSONParser();
    String responseAsString = EntityUtils.toString(response.getEntity());
    JSONObject marcResponse = null;
    try {
      marcResponse= (JSONObject) parser.parse(responseAsString);
    } catch (ParseException pe) {
      marcResponse = new JSONObject();
      marcResponse.put("wrappedErrorMessage", responseAsString);
    }
    response.close();
    if(response.getStatusLine().getStatusCode() != 201) {
      logger.error(String.format("Got error %s, %s adding record: %s",
              response.getStatusLine().getStatusCode(),
              responseAsString,
              marcPostJson.toJSONString()));
      throw new IOException(String.format("Error adding record %s: %s (%s)",
              marcJson.toJSONString(),
              responseAsString,
              response.getStatusLine().getStatusCode()));
    } else {
      logger.debug("Status code: " + response.getStatusLine().getStatusCode()
          + " for POST of marc json " + marcPostJson.toJSONString());
    }
    return marcResponse;
  }

  /**
   * POST instance to Inventory
   * @param instanceRecord
   * @return
   * @throws UnsupportedEncodingException
   * @throws IOException
   * @throws ParseException
   */
  private JSONObject addInstanceRecord(JSONObject instanceRecord) {
    JSONObject instanceResponse = null;
    instancesProcessed++;
    try {
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
      try {
        instanceResponse= (JSONObject) parser.parse(responseAsString);
      } catch (ParseException pe) {
        instanceResponse = new JSONObject();
        instanceResponse.put("wrappedErrorMessage", responseAsString);
      }
      response.close();
      if(response.getStatusLine().getStatusCode() != 201 && response.getStatusLine().getStatusCode() != 200) {
        instancesFailed++;
        String errorMessage = response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase();
        if (instanceExceptionCounts.containsKey(errorMessage)) {
          instanceExceptionCounts.put(errorMessage, instanceExceptionCounts.get(errorMessage)+1);
        } else {
          instanceExceptionCounts.put(errorMessage, 1);
        }
        if (instanceExceptionCounts.get(errorMessage) % 100 == 0) {
          logger.error(String.format("%d instances failed with %s", instanceExceptionCounts.get(errorMessage),errorMessage));
        }
        logger.debug(String.format("Got error %s, %s adding record: %s",
                response.getStatusLine().getStatusCode(),
                responseAsString,
                instanceRecord.toJSONString()));
      } else {
        ((InventoryStorageStatus) storageStatus).incrementAdd(1);
        instancesLoaded++;
      }
    } catch (IOException | org.apache.http.ParseException e) {
      instancesFailed++;
      String errorMessage = String.format("Got error adding Instance record: %s", e.getLocalizedMessage());
      if (instanceExceptionCounts.containsKey(errorMessage)) {
        instanceExceptionCounts.put(errorMessage, instanceExceptionCounts.get(errorMessage)+1);
      } else {
        instanceExceptionCounts.put(errorMessage, 1);
      }
      if (instanceExceptionCounts.get(errorMessage) % 100 == 0) {
        logger.error(String.format("%d instances failed with %s", instanceExceptionCounts.get(errorMessage),errorMessage));
      }
      logger.debug("Error storing Instance record: %s " + e.getLocalizedMessage());
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
  private JSONObject addHoldingsRecord(JSONObject holdingsRecord) {
    holdingsRecordsProcessed++;
    String url = folioAddress + getConfigurationValue(HOLDINGS_STORAGE_PATH);
    HttpPost httpPost = new HttpPost(url);
    StringEntity entity = new StringEntity(holdingsRecord.toJSONString(),"UTF-8");
    httpPost.setEntity(entity);
    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("Content-type", "application/json");
    httpPost.setHeader("X-Okapi-Token", authToken);
    httpPost.setHeader("X-Okapi-Tenant", getConfigurationValue(FOLIO_TENANT));
    JSONObject holdingsRecordResponse = null;
    try {
      CloseableHttpResponse response = client.execute(httpPost);
      JSONParser parser = new JSONParser();
      String responseAsString = EntityUtils.toString(response.getEntity());
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
      } else {
        holdingsRecordsLoaded++;
      }
    } catch (IOException | org.apache.http.ParseException e) {
      holdingsRecordsFailed++;
      logger.error(String.format("Got error adding holdingsRecord: %s", e.getLocalizedMessage()));
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
  private JSONObject addItem(JSONObject item) {
    itemsProcessed++;
    String url = folioAddress + getConfigurationValue(ITEM_STORAGE_PATH);
    HttpPost httpPost = new HttpPost(url);
    StringEntity entity = new StringEntity(item.toJSONString(),"UTF-8");
    httpPost.setEntity(entity);
    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("Content-type", "application/json");
    httpPost.setHeader("X-Okapi-Token", authToken);
    httpPost.setHeader("X-Okapi-Tenant", getConfigurationValue(FOLIO_TENANT));
    JSONObject itemResponse = null;
    try {
      CloseableHttpResponse response = client.execute(httpPost);
      JSONParser parser = new JSONParser();
      String responseAsString = EntityUtils.toString(response.getEntity());
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
      } else {
        itemsLoaded++;
      }
    } catch (IOException | org.apache.http.ParseException e) {
      itemsFailed++;
      logger.error(String.format("Got error adding item record: %s", e.getLocalizedMessage()));
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
