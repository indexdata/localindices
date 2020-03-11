package com.indexdata.masterkey.localindices.harvest.storage;

import static java.util.Comparator.comparing;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

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
import org.xml.sax.SAXException;

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
  private int sourceRecordsProcessed = 0;
  private int sourceRecordsLoaded = 0;
  private int sourceRecordsFailed = 0;
  private final ExecutionTimeStats timingsEntireRecord = new ExecutionTimeStats();
  private final ExecutionTimeStats timingsStoreInstance = new ExecutionTimeStats();


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
    String instancesMessage = "Instances processed/loaded/failed: " + instancesProcessed + "/" + instancesLoaded + "/" + instancesFailed + ". ";
    String holdingsRecordsMessage = "Holdings records processed/loaded/failed: " + holdingsRecordsProcessed + "/" + holdingsRecordsLoaded + "/" + holdingsRecordsFailed + ". ";
    String itemsMessage = "Items processed/loaded/failed: " + itemsProcessed + "/" + itemsLoaded + "/" + itemsFailed + ".";
    String sourceRecordsMessage = "Source records processed/loaded/failed: " + sourceRecordsProcessed + "/" + sourceRecordsLoaded + "/" + sourceRecordsFailed + ".";

    logger.log((instancesFailed>0 ? Level.WARN : Level.INFO), instancesMessage);
    logger.log((holdingsRecordsFailed>0 ? Level.WARN : Level.INFO), holdingsRecordsMessage);
    logger.log((itemsFailed>0 ? Level.WARN : Level.INFO), itemsMessage);
    logger.log((sourceRecordsFailed>0 ? Level.WARN : Level.INFO), sourceRecordsMessage);

    for (String key : instanceExceptionCounts.keySet()) {
      logger.info(String.format("%d Instance records failed with %s", instanceExceptionCounts.get(key),key));
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
          processAddRecord(subRecord);
        }
      } else { // Cannot get original content with a collection of multiple records
        if (harvestable.isStoreOriginal()) {
          logger.warn("Store original content selected for this job, "
                  + "but storage layer received "
                  + "result wiht multiple metadata records and original content "
                  + "cannot be stored in that scenario.");
        }
        for (Record subRecord : subrecords) {
          processAddRecord(subRecord);
        }
      }
    } else {
      processAddRecord(recordJson);
    }
  }

  private void processAddRecord(Record recordJson) {
    long startStorageEntireRecord = System.currentTimeMillis();
    JSONObject instanceWithHoldingsAndItems = ((RecordJSON)recordJson).toJson();
    if (instanceWithHoldingsAndItems.containsKey("title")) {
      JSONObject instanceResponse = addInstanceHoldingsRecordsAndItems(instanceWithHoldingsAndItems);
      if (instanceResponse != null && harvestable.isStoreOriginal()) {
        sourceRecordsProcessed++;
        JSONObject marcJson = getMarcJson((RecordJSON)recordJson);
        addMarcRecord(marcJson, (String)instanceResponse.get("id"));
      }
      timingsEntireRecord.time(startStorageEntireRecord);
      if (instanceResponse != null && instancesLoaded % (instancesLoaded<1000 ? 100 : 1000) == 0) {
        logger.info("" + instancesLoaded + " instances, " + holdingsRecordsLoaded + " holdings records, " + itemsLoaded + " items, and " + sourceRecordsLoaded + " source records ingested.");
        if (instancesFailed+holdingsRecordsFailed+itemsFailed>0) {
          logger.info("Failed: " + instancesFailed + " instances, " + holdingsRecordsFailed + " holdings records, " + itemsFailed + " items, and " + sourceRecordsFailed + " source records.");
        }
      }
    } else {
      if (recordJson.isDeleted()) {
        logger.info("Record is deleted: [" + recordJson.getId() + "], [" + ((RecordJSON)recordJson).toJson() + "]");
      } else {
        logger.info("Inventory record storage received instance record that was not a delete but also with no 'title' property, ["+((RecordJSON)recordJson).toJson() +"] cannot create in Inventory, skipping. ");
      }
    }
  }

  private JSONObject getMarcJson(RecordJSON record) {
    JSONObject marcJson = null;
    if (record.getOriginalContent() != null) {
      try {
      logger.debug(this.getClass().getSimpleName() + " originalContent to store for Record with a collection of " + record.getSubRecords().size() + " record(s):" +  new String(record.getOriginalContent(), "UTF-8"));
      marcJson = MarcXMLToJson.convertMarcXMLToJson(new String(record.getOriginalContent(), "UTF-8"));
      logger.debug(marcJson.toJSONString());
      } catch (IOException | ParserConfigurationException | SAXException e) {
        logger.error("Error creating MARC JSON for source record: " + e.getLocalizedMessage());
        sourceRecordsFailed++;
      }
    } else {
      if (harvestable.isStoreOriginal()) {
        logger.error("Job set to store original source but no original content found.");
        sourceRecordsFailed++;
      }
    }
    return marcJson;
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
          String institutionId = getInstitutionId(holdingsRecords);
          deleteHoldingsAndItemsForInstitution(instanceId, institutionId);
          addHoldingsRecordsAndItems(holdingsRecords, instanceId);
        } catch (ParseException | ClassCastException | IOException e) {
          logger.error("Error adding holdings record and/or items: " + e.getLocalizedMessage());
          holdingsRecordsFailed++;
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
            if (holdingsRecordResponse != null && holdingsRecordResponse.get("id") != null) {
              Iterator itemsIterator = items.iterator();
              while (itemsIterator.hasNext()) {
                JSONObject item = (JSONObject) itemsIterator.next();
                item.put("holdingsRecordId", holdingsRecordResponse.get("id").toString());
                addItem(item);
              }
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
   * Wipes out existing holdings and items belonging to the institution
   * @param instanceId
   * @throws IOException
   * @throws ParseException
   */
  private void deleteHoldingsAndItemsForInstitution (String instanceId, String institutionId) throws IOException, ParseException {
    logger.debug("Deleting holdings and items for Instance Id " + instanceId + " for institution " + institutionId);
    int itemsToDelete = 0;
    if (institutionId != null) {
      JSONArray existingHoldingsRecords = getHoldingsRecordsByInstanceId(instanceId);
      if (existingHoldingsRecords != null) {
        Iterator<JSONObject> existingHoldingsRecordsIterator = existingHoldingsRecords.iterator();
        while (existingHoldingsRecordsIterator.hasNext()) {
          JSONObject existingHoldingsRecord = existingHoldingsRecordsIterator.next();
          String institutionIdExistingHoldingsRecord = getInstitutionId(existingHoldingsRecord);
          String existingHoldingsRecordId = (String) existingHoldingsRecord.get("id");
          if (institutionIdExistingHoldingsRecord.equals(institutionId)) {
            JSONArray items = getItemsByHoldingsRecordId(existingHoldingsRecordId);
            if (items != null) {
              itemsToDelete = items.size();
              Iterator<JSONObject> itemsIterator = items.iterator();
              while (itemsIterator.hasNext()) {
                JSONObject item = itemsIterator.next();
                String itemId = (String) item.get("id");
                deleteItem(itemId);
              }
            }
            try {
              deleteHoldingsRecord(existingHoldingsRecordId);
            } catch (IOException ioe) {
              if (ioe.getMessage().contains("still referenced")) {
                logger.info("Holdings record for deletion: " + existingHoldingsRecord.toJSONString() + " had " + itemsToDelete + " items.");
                logger.info("Items referencing the holdings record: " + getItemsByHoldingsRecordId(existingHoldingsRecordId).toJSONString());
              }
              throw ioe;
            }
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
   * Gets an instance from FOLIO Inventory by identifier (type and value)
   * @param localIdentifier  Identifier value
   * @param identifierTypeId  Inventory identifier type ID
   * @return
   * @throws IOException
   * @throws ParseException
   */
  private JSONObject getInstance (String localIdentifier, String identifierTypeId)
    throws IOException, ParseException {
    String url = String.format("%s?query=%%28identifiers%%20%%3D%%2F%%40value%%2F%%40identifierTypeId%%3D%%22%s%%22%%20%%22%s%%22%%29", folioAddress + "instance-storage/instances", identifierTypeId, localIdentifier);
    HttpGet httpGet = new HttpGet(url);
    httpGet.setHeader("Accept", "application/json");
    httpGet.setHeader("Content-type", "application/json");
    httpGet.setHeader("X-Okapi-Token", authToken);
    httpGet.setHeader("X-Okapi-Tenant", getConfigurationValue(FOLIO_TENANT));
    CloseableHttpResponse response = client.execute(httpGet);
    if(! Arrays.asList(200, 404).contains(response.getStatusLine().getStatusCode())) {
      throw new IOException(String.format("Got error retrieving instance by local identifier %s and identifierTypeId %s: %s",
          localIdentifier, identifierTypeId, EntityUtils.toString(response.getEntity())));
    }
    JSONObject jsonResponse;
    JSONParser parser = new JSONParser();
    String responseString = EntityUtils.toString(response.getEntity());
    logger.info("getInstanceId response: " + responseString);
    jsonResponse = (JSONObject) parser.parse(responseString);
    Long totalRecords = (Long)jsonResponse.getOrDefault("totalRecords",0);
    if (totalRecords == 1 && jsonResponse.get("instances") != null) {
      return ((JSONObject)((JSONArray) jsonResponse.get("instances")).get(0));
    } else {
      logger.info("totalRecords for instance query by identifier was " + totalRecords);
      return null;
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
    String url = String.format("%s?limit=1000&query=instanceId%%3D%%3D%s", folioAddress + getConfigurationValue(HOLDINGS_STORAGE_PATH), instanceId);
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
    String url = String.format("%s?limit=1000&query=holdingsRecordId%%3D%%3D%s", folioAddress + getConfigurationValue(ITEM_STORAGE_PATH), holdingsRecordId);
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
    CloseableHttpResponse response = null;
    try {
      response = client.execute(httpDelete);
      if(response.getStatusLine().getStatusCode() != 204) {
        throw new IOException(String.format("Got error deleting holdingsRecord with id '%s': %s",
            uuid, EntityUtils.toString(response.getEntity())));
      }
    } finally {
      if (response != null) response.close();
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
    CloseableHttpResponse response = null;
    try {
      response = client.execute(httpDelete);
      if(response.getStatusLine().getStatusCode() != 204) {
        throw new IOException(String.format("Got error deleting item with id '%s': %s",
            uuid, EntityUtils.toString(response.getEntity())));
      }
    } finally {
      if (response != null) response.close();
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
    logger.info("Delete request received for record with ID [" + record.getId() + "] "
                + "Delete request argument of type " + record.getClass());
    JSONObject deletionJson = ((record instanceof RecordJSONImpl) ? ((RecordJSON) record).toJson() : null);
    logger.info("Content of deletion record: " + (deletionJson != null ? deletionJson.toJSONString() : " [Record not JSON, cannot display content]"));
    if (deletionJson != null) {
      String oaiId = (String) deletionJson.get("identifier");
      String id = (oaiId != null ? oaiId.substring(oaiId.lastIndexOf(":")+1) : null);
      String identifierTypeId = (String) deletionJson.get("identifierTypeId");
      String permanentLocationId = (String) deletionJson.get("permanentLocationId");
      if (id != null && identifierTypeId != null && permanentLocationId != null) {
        // This is assumed to be a deletion record targeted for a shared inventory
        logger.info("Storage class received a deletion record with ID: [" + id +"], identifierTypeId ["+identifierTypeId+"], permanentLocationId ["+permanentLocationId+"]");
        try {
          JSONObject instance = getInstance(id, identifierTypeId);
          if (instance != null) {
            String instanceId = (String) instance.get("id");
            logger.info("Found instance ID: " + instanceId);
            JSONArray identifiers = (JSONArray)instance.get("identifiers");
            JSONObject identifier = null;
            Iterator iter = identifiers.iterator();
            while (iter.hasNext()) {
              JSONObject identifierObject = (JSONObject) iter.next();
              if (identifierTypeId.equals(identifierObject.get("identifierTypeId"))
                 && id.equals(identifierObject.get("value"))) {
                identifier = identifierObject;
                break;
              }
            }
            identifiers.remove(identifier);
            logger.info("Removed " + identifier.toJSONString() + " from " + instance.toJSONString());
            deleteHoldingsAndItemsForInstitution(instanceId, locationsToInstitutionsMap.get(permanentLocationId));
            updateInstance(instance);
          } else {
            logger.info("No instance found for local id ["+id+"] and identifierTypeId ["+identifierTypeId+"]. Cannot perform delete.");
          }

        } catch (IOException ioe) {
          logger.error(ioe.getMessage());
        } catch (ParseException pe) {
          logger.error(pe.getMessage());
        }
      } else if (id != null) {
        // This is assumed to be a deletion record targeted for a simple inventory
        logger.info("Storage class received a deletion record with ID: [" + id +"]");
      } else if (oaiId != null && id == null) {
        logger.error("ID not found in the OAI identifier [" + oaiId + "]. Cannot perform delete against Inventory");
      } else if (oaiId == null) {
        logger.error("No OAI identifier found in deletion record. Cannot perform delete against Inventory");
      }
    }
  }

  /**
   * PUTs the instance object argument to Inventory
   * @param instance
   */
  public void updateInstance (JSONObject instance) {
    logger.info("Updating Instance with " + instance.toJSONString());
    try {
      String url = folioAddress + "instance-storage/instances/" + instance.get("id");
      HttpEntityEnclosingRequestBase httpUpdate;
      httpUpdate = new HttpPut(url);
      StringEntity entity = new StringEntity(instance.toJSONString(),"UTF-8");
      httpUpdate.setEntity(entity);
      httpUpdate.setHeader("Accept", "text/plain");
      httpUpdate.setHeader("Content-type", "application/json");
      httpUpdate.setHeader("X-Okapi-Token", authToken);
      httpUpdate.setHeader("X-Okapi-Tenant", getConfigurationValue(FOLIO_TENANT));
      CloseableHttpResponse response = client.execute(httpUpdate);
      response.close();
      logger.info("Updated instance " + instance.get("id"));
    } catch (IOException ioe) {
      logger.error("IO error updating instance: " + ioe.getMessage());
    }
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

  private JSONObject addMarcRecord(JSONObject marcJson, String instanceId)  {
    JSONObject marcResponse = null;
    if (marcJson != null) {
      try {
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
        sourceRecordsFailed++;
      } else {
        logger.debug("Status code: " + response.getStatusLine().getStatusCode()
            + " for POST of marc json " + marcPostJson.toJSONString());
        sourceRecordsLoaded++;
      }
      } catch (IOException | org.apache.http.ParseException | UnsupportedCharsetException e) {
        logger.error("Error adding MARC source record: " + e.getLocalizedMessage());
        sourceRecordsFailed++;
      }
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
        if (instanceExceptionCounts.get(errorMessage) < 10 || instanceExceptionCounts.get(errorMessage) % 100 == 0) {
          logger.error(String.format("%d instances failed with %s", instanceExceptionCounts.get(errorMessage),errorMessage));
        }
        logger.debug(String.format("Got error %s, %s adding Instance record: %s",
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
      if (instanceExceptionCounts.get(errorMessage) < 10 || instanceExceptionCounts.get(errorMessage) % 100 == 0) {
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
        logger.error(String.format("Got error %s, %s adding holdingsRecord: %s",
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
        logger.error(String.format("Got error %s, %s adding item record: %s",
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
      throw new IOException(String.format("Got error deleting instance record with id '%s': %s",
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

  private String getInstitutionId(JSONArray holdingsRecords) {
    String locationId = getLocationId(holdingsRecords);
    if (locationId != null) {
      return locationsToInstitutionsMap.get(locationId);
    } else {
      return null;
    }
  }

  private String getInstitutionId(JSONObject holdingsRecord) {
    if (holdingsRecord != null) {
      String locationId = getLocationId(holdingsRecord);
      if (locationId != null) {
        return locationsToInstitutionsMap.get(locationId);
      }
    }
    return null;
  }

  private String getLocationId(JSONArray holdingsRecords) {
    if (holdingsRecords != null && holdingsRecords.get(0) instanceof JSONObject ) {
      return getLocationId((JSONObject)(holdingsRecords.get(0)));
    } else {
      return null;
    }
  }

  private String getLocationId(JSONObject holdingsRecord) {
    if (holdingsRecord != null) {
      return (String) holdingsRecord.get("permanentLocationId");
    } else {
      return null;
    }
  }


  private class ExecutionTimeStats {
    private final SimpleDateFormat HOUR = new SimpleDateFormat("MM-DD:HH");
    private final Map<String, HourStats> execTimes = new HashMap<>();

    HourStats hourstats = null;

    public void time(long start) {
      long end = System.currentTimeMillis();
      String hour = HOUR.format(new Date());
      if (execTimes.containsKey(hour)) {
        execTimes.get(hour).log(start, end);
      } else {
        writeLog(); // at top of the hour, write logs up until previous hour
        hourstats = new HourStats();
        hourstats.log(start, end);
        execTimes.put(hour,hourstats);
      }
    }

    public void writeLog() {
      execTimes.entrySet().stream()
      .sorted(comparing(Entry::getKey))
      .forEach(e -> { // for each hour
        HourStats hr = e.getValue();
        StringBuilder totals1 = new StringBuilder();
        totals1.append(e.getKey()).append(": ")
               .append(hr.execCount).append(" records processed in ").append(hr.totalExecTime/1000).append(" secs.")
               .append("~").append(hr.totalExecTime/60000).append(" mins. of execution time");
        logger.info(totals1.toString());

        StringBuilder totals2 = new StringBuilder();
        totals2.append("Average: ").append(hr.totalExecTime/hr.execCount).append(" ms. ")
               .append("Fastest: ").append(hr.minExecTime).append(" ms. ")
               .append("Slowest: ").append(hr.maxExecTime).append(" ms. ");
        logger.info(totals2.toString());

        e.getValue().execTimeIntervals.entrySet().stream()
                .sorted(comparing(Entry::getKey))
                .forEach(f -> { // for each response time interval
                  StringBuilder intv = new StringBuilder();
                  intv.append("Up to ").append(f.getKey()).append(" ms for ").append(f.getValue()).append(" records. ");
                  if (f.getValue()*100/hr.execCount>0)
                    intv.append("(").append(f.getValue()*100/hr.execCount).append("%)");
                  logger.info(intv.toString());
                });}
      );
    }
  }

  private class HourStats {
    int execCount = 0;
    long totalExecTime = 0;
    long maxExecTime = 0;
    long minExecTime = Long.MAX_VALUE;
    Map <Long, Integer> execTimeIntervals = new HashMap<>();
    Map <String, Long> execTimeByOperation = new HashMap<>();

    public void log (long start, long end) {
      long execTime = end - start;
      execCount++;
      totalExecTime += execTime;
      maxExecTime = Math.max(maxExecTime, execTime);
      minExecTime = Math.min(minExecTime, execTime);
      long execTimeRounded = execTime<1000 ? ((execTime + 99) / 100) * 100 :
                                                       ((execTime + 999) / 1000) * 1000;

      if (execTimeIntervals.containsKey(execTimeRounded)) {
        execTimeIntervals.put(execTimeRounded,execTimeIntervals.get(execTimeRounded)+1);
      } else {
        execTimeIntervals.put(execTimeRounded,1);
      }
    }
  }

}
