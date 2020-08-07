package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.RecordJSON;
import com.indexdata.masterkey.localindices.harvest.storage.StorageException;
import com.indexdata.masterkey.localindices.util.MarcXMLToJson;

/**
 * Logic for handling the create/update/delete for one FOLIO Inventory record
 * set -- an Inventory record set being an instance with
 * holdings/items and possibly a source record, all created from one
 * incoming bibliographic record.
 *
 * InventoryRecordUpdate gets context from {@link InventoryStorageController},
 * for example the job logger and record update/failure counters for logging the
 * process.
 *
 *
 *
 * @author ne
 */
 @SuppressWarnings("unchecked")
 public class InventoryRecordUpdater {

  private final StorageJobLogger logger;
  /** Container for errors encountered while updating Inventory from one incoming bib record */
  private RecordWithErrors recordWithErrors;
  /** Overall updates and errors counters */
  private final RecordUpdateCounters updateCounters;
  private final FailedRecordsController failedRecordsController;
  private final Map<String, String> locInstMap;
  private final InventoryUpdateContext ctxt;

  public InventoryRecordUpdater (InventoryUpdateContext ctxt) {
    this.ctxt = ctxt;
    logger = ctxt.logger;
    failedRecordsController = ctxt.failedRecordsController;
    updateCounters = ctxt.updateCounters;
    locInstMap = ctxt.locationsToInstitutionsMap;
  }

  /**
   * Adds or updates a set of Inventory records (an Instance with related holdings and/or a source record)
   * from the incoming bibliographic record.
   * @param recordJSON
   */
  void addInventory(RecordJSON recordJSON) {
    try {
      long startStorageEntireRecord = System.currentTimeMillis();
      /**
       * TransformedRecord is a 'facade' that provides an API into RecordJSON and allows for (slight)
       * variations in the structure of the transformed record while shielding the client code from
       * those potential differences in structure. Thus safer to use than RecordJSON.
       */
      TransformedRecord transformedRecord = new TransformedRecord(recordJSON, logger);
      this.recordWithErrors = new RecordWithErrors(transformedRecord, failedRecordsController);

      if (ctxt.useInventoryUpsert) {
        JSONObject inventoryRecordSet = new JSONObject();
        JSONObject instance = transformedRecord.getInstance();
        if (transformedRecord.hasMatchKey() && ctxt.inventoryUpsertPath.contains("matchkey")) {
          instance.put("matchKey", transformedRecord.getMatchKey());
        }
        inventoryRecordSet.put("instance", transformedRecord.getInstance());
        inventoryRecordSet.put("holdingsRecords", transformedRecord.getHoldings());
        JSONObject responseJson = upsertInventoryRecordSet(inventoryRecordSet);
        UpsertMetrics metrics = new UpsertMetrics((JSONObject)responseJson.get("metrics"));

        if (ctxt.harvestable.isStoreOriginal()) {
          if (metrics.instance.create.failed==0) {
            String institutionId = transformedRecord.getInstitutionId(locInstMap);
            String localIdentifier = transformedRecord.getLocalIdentifier();

            updateCounters.sourceRecordsProcessed++;
            if (ctxt.marcStorageUrlIsDefined) {
              JSONObject marcJson = getMarcJson(transformedRecord);
              JSONObject instanceJson = (JSONObject) responseJson.get("instance");
              String instanceId = instanceJson.get("id").toString();
              addOrUpdateMarcRecord(marcJson, instanceId, institutionId, localIdentifier);
            } else {
              updateCounters.sourceRecordsFailed++;
              RecordError error = new ExceptionRecordError(
                new InventoryUpdateException("Configuration error: Cannot store original content as requested, no path configured for MARC storage"),
                "Missing configuration: [" + InventoryUpdateContext.MARC_STORAGE_PATH + "]", InventoryUpdateContext.FAILURE_ENTITY_TYPE_SOURCE_RECORD);
              recordWithErrors.reportError(error, Level.DEBUG);
            }
          } else {
            RecordError error = new ExceptionRecordError(
              new InventoryUpdateException("Instance error: Cannot store original content as requested since Instance creation failed"),
              "Missing Instance, source record skipped: [" + InventoryUpdateContext.MARC_STORAGE_PATH + "]", InventoryUpdateContext.FAILURE_ENTITY_TYPE_SOURCE_RECORD);
            recordWithErrors.reportError(error, Level.DEBUG);

          }
        }
        ctxt.timingsStoringInventoryRecordSet.time(startStorageEntireRecord);
        ctxt.timingsCreatingRecord.setTiming(recordJSON.getCreationTiming());
        ctxt.timingsTransformingRecord.setTiming(recordJSON.getTransformationTiming());
        ctxt.storageStatus.incrementAdd(1);
        setCounters(metrics);
        logRecordCounts();
      } else {
        // TODO: eventually deprecate this section and supporting methods
        JSONObject instanceResponse;
        if (ctxt.instanceStoragePath.contains("match")) {
          instanceResponse = addInstanceRecord(transformedRecord.getInstance(), transformedRecord.getMatchKey());
        } else {
          instanceResponse = addInstanceRecord(transformedRecord.getInstance());
        }

        if (instanceResponse != null && instanceResponse.get("id") != null) {
          String institutionId = transformedRecord.getInstitutionId(locInstMap);
          String localIdentifier = transformedRecord.getLocalIdentifier();
          try {
            updateHoldings(transformedRecord, instanceResponse, institutionId);
          } catch (InventoryUpdateException iue) {
            // there were errors updating holdings/items - but continue with handling source record (if isStoreOriginal)
          }
          if (ctxt.harvestable.isStoreOriginal()) {
            updateCounters.sourceRecordsProcessed++;
            if (ctxt.marcStorageUrlIsDefined) {
              JSONObject marcJson = getMarcJson(transformedRecord);
              addOrUpdateMarcRecord(marcJson, (String) instanceResponse.get("id"), institutionId, localIdentifier);
            } else {
              updateCounters.sourceRecordsFailed++;
              RecordError error = new ExceptionRecordError(
                new InventoryUpdateException("Configuration error: Cannot store original content as requested, no path configured for MARC storage"),
                "Missing configuration: [" + InventoryUpdateContext.MARC_STORAGE_PATH + "]", InventoryUpdateContext.FAILURE_ENTITY_TYPE_SOURCE_RECORD);
              recordWithErrors.reportAndThrowError(error, Level.DEBUG);
            }
          }
          ctxt.timingsStoringInventoryRecordSet.time(startStorageEntireRecord);
          ctxt.timingsCreatingRecord.setTiming(recordJSON.getCreationTiming());
          ctxt.timingsTransformingRecord.setTiming(recordJSON.getTransformationTiming());
          logRecordCounts();
        } else {
          if (transformedRecord.isDeleted()) {
            logger.error("Deletion record received on 'add' channels: [" + transformedRecord.getLocalIdentifier() + "], [" + transformedRecord.getJson() + "]");
          } else {
            logger.error("Expected instance response on adding instance but response was null or had no ID property." + instanceResponse);
          }
        }
        if (recordWithErrors.hasErrors())  {
          recordWithErrors.writeErrorsLog(logger);
        }
      }
    } catch (InventoryUpdateException iue) {
        recordWithErrors.writeErrorsLog(logger);
    }
  }

  private void logRecordCounts() {
    if (updateCounters.instancesLoaded>0 && updateCounters.instancesLoaded % (updateCounters.instancesLoaded < 1000 ? 100 : 1000) == 0) {
      logger.info("" + updateCounters.instancesLoaded + " instances, " + updateCounters.holdingsRecordsLoaded + " holdings records, " + updateCounters.itemsLoaded + " items, and " + updateCounters.sourceRecordsLoaded + " source records ingested. " + updateCounters.instanceDeleteSignals + " delete signal(s), " + updateCounters.instanceDeletions + " delete(s)");
      if (updateCounters.instancesFailed + updateCounters.holdingsRecordsFailed + updateCounters.itemsFailed > 0) {
        logger.info("Failed: " + updateCounters.instancesFailed + " instances, " + updateCounters.holdingsRecordsFailed + " holdings records, " + updateCounters.itemsFailed + " items, and " + updateCounters.sourceRecordsFailed + " source records.");
      }
    }
  }

  private void setCounters (UpsertMetrics metrics) {
    updateCounters.holdingsRecordsDeleted += metrics.holdingsRecord.delete.completed;
    updateCounters.holdingsRecordsFailed += metrics.holdingsRecord.failed;
    updateCounters.holdingsRecordsLoaded += metrics.holdingsRecord.create.completed + metrics.holdingsRecord.update.completed;
    updateCounters.holdingsRecordsProcessed += metrics.holdingsRecord.processed;
    updateCounters.instanceDeleteSignals += metrics.instance.delete.processed;
    updateCounters.instanceDeletions += metrics.instance.delete.completed;
    updateCounters.instancesFailed += metrics.instance.failed;
    updateCounters.instancesLoaded += metrics.instance.create.completed + metrics.instance.update.completed;
    updateCounters.instancesProcessed += metrics.instance.processed;
    updateCounters.itemsDeleted += metrics.item.delete.completed;
    updateCounters.itemsFailed += metrics.item.failed;
    updateCounters.itemsLoaded += metrics.item.create.completed + metrics.item.update.completed;
    updateCounters.itemsProcessed += metrics.item.processed;
  }

  public class UpsertMetrics {

    EntityMetrics instance;
    EntityMetrics holdingsRecord;
    EntityMetrics item;

    public UpsertMetrics(JSONObject upsertMetricsJson) {
      JSONObject json = new JSONObject();
      if (upsertMetricsJson != null) {
        json = upsertMetricsJson;
      }
      instance = new EntityMetrics((JSONObject) json.get("INSTANCE"));
      holdingsRecord = new EntityMetrics((JSONObject) json.get("HOLDINGSRECORD"));
      item = new EntityMetrics((JSONObject) json.get("ITEM"));
    }

  }

  public class EntityMetrics {

    TransactionMetrics create;
    TransactionMetrics update;
    TransactionMetrics delete;
    long processed = 0;
    long failed = 0;
    public EntityMetrics (JSONObject entityMetricsJson) {
      JSONObject json = new JSONObject();
      if (entityMetricsJson != null) {
        json = entityMetricsJson;
      }
      create = new TransactionMetrics((JSONObject) json.get("CREATE"));
      update = new TransactionMetrics((JSONObject) json.get("UPDATE"));
      delete = new TransactionMetrics((JSONObject) json.get("DELETE"));
      processed = create.processed + update.processed;
      failed = create.failed + update.failed + delete.failed;
    }

  }

  public class TransactionMetrics {

    Long completed = new Long(0);
    Long failed = new Long(0);
    Long skipped = new Long(0);
    Long processed = new Long(0);

    public TransactionMetrics (JSONObject transactionMetricsJson) {

      if (transactionMetricsJson != null) {
        completed =  (Long) transactionMetricsJson.get("COMPLETED");
        failed = (Long) transactionMetricsJson.get("FAILED");
        skipped = (Long) transactionMetricsJson.get("SKIPPED");
        processed = completed+failed+skipped;
      }
    }

  }

  /**
   * Updates holdings/items for given instance - by wiping out existing holdings/items and creating new ones as received
   * with the harvested bib record.
   * @param transformedRecord  representation of the harvested, normalized bib record
   * @param instanceResponse the instance to update holdings/items for
   * @param institutionId id of the institution who holds the items on the instance to be updated
   * @throws InventoryUpdateException
   */
  private void updateHoldings(TransformedRecord transformedRecord, JSONObject instanceResponse, String institutionId) throws InventoryUpdateException {
    JSONArray holdingsJson = transformedRecord.getHoldings();
    if (holdingsJson != null && holdingsJson.size() > 0) {
      String instanceId = instanceResponse.get("id").toString();
      // delete existing holdings/items from the same institution
      // before attaching new holdings/items to the instance
      deleteHoldingsAndItemsForInstitution(instanceId, institutionId, false);
      addHoldingsRecordsAndItems(holdingsJson, instanceId);
    }
  }

  /**
   * Adds an Instance (without applying matchkey logic)
   * @param instanceRecord
   * @return Inventory's response to the request
   * @throws InventoryUpdateException
   */
  private JSONObject addInstanceRecord(JSONObject instanceRecord) throws InventoryUpdateException {
    JSONObject noMatchKey = new JSONObject();
    return addInstanceRecord(instanceRecord, noMatchKey);
  }

  private JSONObject upsertInventoryRecordSet(JSONObject inventoryRecordSet) throws InventoryUpdateException {
    JSONObject upsertResponse = null;
    try {
      String url = ctxt.folioAddress + ctxt.inventoryUpsertPath;
      HttpEntityEnclosingRequestBase httpUpdate = new HttpPut(url);
      StringEntity entity = new StringEntity(inventoryRecordSet.toJSONString(), "UTF-8");
      httpUpdate.setEntity(entity);
      setHeaders(httpUpdate,"application/json");
      CloseableHttpResponse response = ctxt.inventoryClient.execute(httpUpdate);

      String responseAsString = EntityUtils.toString(response.getEntity());
      response.close();
      upsertResponse = getResponseAsJson(responseAsString);
      checkForNoSuitableModulePath(response, responseAsString);
      checkForRecordErrors(response, upsertResponse);
    } catch (StorageException se) {
      throw se;
    } catch (Exception e) {
      throw new InventoryUpdateException("Inventory Upsert encountered an exception", e);
    }
    return upsertResponse;
  }

  private JSONObject getResponseAsJson(String responseAsString) {
    JSONObject upsertResponse;
    JSONParser parser = new JSONParser();
    try {
      upsertResponse = (JSONObject) parser.parse(responseAsString);
    } catch (ParseException pe) {
      upsertResponse = new JSONObject();
      upsertResponse.put("wrappedErrorMessage", responseAsString);
    }
    return upsertResponse;
  }

  private void checkForRecordErrors(CloseableHttpResponse response, JSONObject upsertResponse) {
    if (upsertResponse.containsKey("errors")) {
      JSONArray errorsArray = (JSONArray) upsertResponse.get("errors");
      JSONObject firstError = (JSONObject) errorsArray.get(0);
      RecordError error = new HttpRecordError(response.getStatusLine().getStatusCode(),
                                              response.getStatusLine().getReasonPhrase(),
                                              firstError.get("shortMessage").toString() + ": " + firstError.get("entity").toString(),
                                              firstError.get("shortMessage").toString(),
                                              "Error upserting Inventory record set",
                                              firstError.get("entityType").toString());
      recordWithErrors.reportError(error, Level.DEBUG);
    }
  }

  private void checkForNoSuitableModulePath(CloseableHttpResponse response, String responseAsString) {
    if (response.getStatusLine().getStatusCode() == 404) {
      updateCounters.instancesFailed++;
      RecordError error = new HttpRecordError(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase(), responseAsString, responseAsString,"Error upserting Inventory record set", "InventoryRecordSet");
      recordWithErrors.reportError(error, Level.DEBUG);
      logger.debug(String.format("Error %s, %s upserting Inventory record set", response.getStatusLine().getStatusCode(), responseAsString));
      if (responseAsString.contains("No suitable module found for path")) {
        throw new StorageException(error.getMessage());
      }
    }
  }

  /**
   * POST/PUT an Instance to Inventory
   * @param instanceRecord
   * @return
   * @throws InventoryUpdateException
   */
  private JSONObject addInstanceRecord(JSONObject instanceRecord, JSONObject matchKey) throws InventoryUpdateException {
    JSONObject instanceResponse = null;
    updateCounters.instancesProcessed++;
    String method = "";
    try {
      String url = ctxt.folioAddress + ctxt.instanceStoragePath;
      HttpEntityEnclosingRequestBase httpUpdate;
      if (url.contains("instance-storage/instances")) { // Vanilla instance storage, assume POST
        httpUpdate = new HttpPost(url);
        method="POST";
      } else { // Otherwise assume FOLIO PUT API that knows whether to PUT or POST to instance storage
        if (url.contains("matchkey") && !matchKey.isEmpty()) {
          instanceRecord.put("matchKey", matchKey);
        }
        httpUpdate = new HttpPut(url);
        method="PUT";
      }
      StringEntity entity = new StringEntity(instanceRecord.toJSONString(), "UTF-8");
      httpUpdate.setEntity(entity);
      setHeaders(httpUpdate,"application/json");
      CloseableHttpResponse response = ctxt.inventoryClient.execute(httpUpdate);
      String responseAsString = EntityUtils.toString(response.getEntity());
      instanceResponse = getResponseAsJson(responseAsString);
      response.close();
      if (response.getStatusLine().getStatusCode() != 201 && response.getStatusLine().getStatusCode() != 200) {
        updateCounters.instancesFailed++;
        RecordError error = new HttpRecordError(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase(), responseAsString, responseAsString,"Error "+method+"ing Instance", "Instance");
        recordWithErrors.reportAndThrowError(error, Level.DEBUG);
        logger.debug(String.format("Got error %s, %s adding Instance record: %s", response.getStatusLine().getStatusCode(), responseAsString, instanceRecord.toJSONString()));
      } else {
        ctxt.storageStatus.incrementAdd(1);
        updateCounters.instancesLoaded++;
      }
    } catch (IOException  e) {
      updateCounters.instancesFailed++;
      RecordError error = new ExceptionRecordError(e, "Error adding Instance record", "Instance");
      recordWithErrors.reportAndThrowError(error, Level.DEBUG);
    }
    return instanceResponse;
  }

  /**
   * PUTs the instance object argument to Inventory
   * @param instance
   * @throws InventoryUpdateException
   */
  private void updateInstance(JSONObject instance) throws InventoryUpdateException {
    logger.debug("Updating Instance with " + instance.toJSONString());
    try {
      String url = ctxt.folioAddress + "instance-storage/instances/" + instance.get("id");
      HttpEntityEnclosingRequestBase httpUpdate;
      httpUpdate = new HttpPut(url);
      StringEntity entity = new StringEntity(instance.toJSONString(), "UTF-8");
      httpUpdate.setEntity(entity);
      setHeaders(httpUpdate,"text/plain");
      CloseableHttpResponse response = ctxt.inventoryClient.execute(httpUpdate);
      response.close();
      logger.debug("Updated instance " + instance.get("id"));
    } catch (IOException ioe) {
      RecordError error = new ExceptionRecordError(ioe,"Error updating instance", "instance");
      recordWithErrors.reportAndThrowError(error, Level.DEBUG);
    }
  }

  /**
   * POST holdings record to Inventory
   * @param holdingsRecord
   * @return
   * @throws InventoryUpdateException
   */
  private JSONObject addHoldingsRecord(JSONObject holdingsRecord) throws InventoryUpdateException {
    updateCounters.holdingsRecordsProcessed++;
    String url = ctxt.folioAddress + ctxt.holdingsStoragePath;
    HttpPost httpPost = new HttpPost(url);
    StringEntity entity = new StringEntity(holdingsRecord.toJSONString(), "UTF-8");
    httpPost.setEntity(entity);
    setHeaders(httpPost,"application/json");
    JSONObject holdingsRecordResponse = null;
    try {
      CloseableHttpResponse response = ctxt.inventoryClient.execute(httpPost);
      JSONParser parser = new JSONParser();
      String responseAsString = EntityUtils.toString(response.getEntity());
      if (response.getStatusLine().getStatusCode() != 201) {
        updateCounters.holdingsRecordsFailed++;
        RecordError error = new HttpRecordError(response.getStatusLine(), responseAsString, responseAsString, "Error adding a holdingsRecord to Inventory", "holdings");
        recordWithErrors.reportAndThrowError(error, Level.DEBUG);
      } else {
        updateCounters.holdingsRecordsLoaded++;
        try {
          holdingsRecordResponse = (JSONObject) parser.parse(responseAsString);
        } catch (ParseException pe) {
          holdingsRecordResponse = new JSONObject();
          holdingsRecordResponse.put("wrappedErrorMessage", responseAsString);
          RecordError error = new ExceptionRecordError(pe, "Error parsing holdingsRecords from response ", "holdings");
          recordWithErrors.reportAndThrowError(error, Level.DEBUG);
        }
      }
      response.close();
    } catch (IOException | org.apache.http.ParseException e) {
      updateCounters.holdingsRecordsFailed++;
      RecordError error = new ExceptionRecordError(e, "Exception while adding holdingsRecord","holdings");
      recordWithErrors.reportAndThrowError(error, Level.DEBUG);
    }
    return holdingsRecordResponse;
  }

  /**
   * Get holdings records for an instance
   * @param instanceId
   * @return
   * @throws InventoryUpdateException
   */
  private JSONArray getHoldingsRecordsByInstanceId(String instanceId) throws InventoryUpdateException {
    String url = String.format("%s?limit=1000&query=instanceId%%3D%%3D%s", ctxt.folioAddress + ctxt.holdingsStoragePath, instanceId);
    HttpGet httpGet = new HttpGet(url);
    setHeaders(httpGet,"application/json");
    CloseableHttpResponse response;
    try {
      response = ctxt.inventoryClient.execute(httpGet);
      if (!Arrays.asList(200, 404).contains(response.getStatusLine().getStatusCode())) {
        String err = String.format("Got error retrieving holdings records for instance with id '%s': %s", instanceId, EntityUtils.toString(response.getEntity()));
        throw new InventoryUpdateException(err);
      }
    } catch (IOException ioe) {
      throw new InventoryUpdateException("IOException when trying to GET holdings records by instance ID", ioe);
    }
    JSONArray holdingsRecordsJson;
    try {
      JSONObject jsonResponse;
      JSONParser parser = new JSONParser();
      String responseString = EntityUtils.toString(response.getEntity());
      jsonResponse = (JSONObject) parser.parse(responseString);
      holdingsRecordsJson = (JSONArray) (jsonResponse.get("holdingsRecords"));
    } catch (IOException ioe) {
      throw new InventoryUpdateException("IO exception when trying to read holdings records by instance ID response", ioe);
    } catch (ParseException pe) {
      throw new InventoryUpdateException("Could not parse holdings records as JSONArray when trying to get holdings records by Instance ID", pe);
    }
    return holdingsRecordsJson;
  }

  /**
   * Wipes out existing holdings and items belonging to the institution, for the given instance
   * @param instanceId
   * @throws InventoryUpdateException
   */
  private void deleteHoldingsAndItemsForInstitution(String instanceId, String institutionId, boolean countDeletions) throws InventoryUpdateException {
    logger.log(Level.TRACE,"Deleting holdings and items for Instance Id " + instanceId + " for institution " + institutionId);
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
                if (countDeletions) {
                  updateCounters.itemsDeleted++;
                }
              }
            }
            try {
              deleteHoldingsRecord(existingHoldingsRecordId);
              if (countDeletions) {
                updateCounters.holdingsRecordsDeleted++;
              }
            } catch (InventoryUpdateException e) {
              // catch, add diagnostics to message and rethrow
              if (e.getCause() != null && e.getCause().getMessage().contains("still referenced")) {
                String err = "Holdings record for deletion: " + existingHoldingsRecord.toJSONString() + " had " + itemsToDelete + " items.";
                err += "Items referencing the holdings record: " + getItemsByHoldingsRecordId(existingHoldingsRecordId).toJSONString();
                throw new InventoryUpdateException(e.getMessage() + " " + err, e);
              } else {
                throw e;
              }
            }
          } else {
            logger.debug("holdingsRecord " + existingHoldingsRecordId + " belongs to a different institution (" + existingHoldingsRecord.get("permanentLocationId") + "), not deleting it.");
          }
        }
      } else {
        logger.info("No existing holdingsRecords found for the instance, nothing to delete.");
      }
    }
  }

  /**
   * Delete an item by ID
   * @param uuid
   * @throws InventoryUpdateException
   */
  private void deleteItem(String uuid) throws InventoryUpdateException {
    logger.log(Level.TRACE, "Deleting item with ID: " + uuid);
    String url = String.format("%s/%s", ctxt.folioAddress + ctxt.itemStoragePath, uuid);
    HttpDelete httpDelete = new HttpDelete(url);
    setHeaders(httpDelete,"text/plain");
    CloseableHttpResponse response = null;
    try {
      response = ctxt.inventoryClient.execute(httpDelete);
      if (response.getStatusLine().getStatusCode() != 204) {
        RecordError error = new HttpRecordError(response.getStatusLine(), response.toString(), response.toString(), "Error DELETEing item", "item");
        recordWithErrors.reportAndThrowError(error, Level.ERROR);
      }
    } catch (IOException e) {
      RecordError error = new ExceptionRecordError(e, "IOException when attempting to DELETE item", "item");
      recordWithErrors.reportAndThrowError(error, Level.ERROR, e);
    } finally {
      if (response != null) {
        try {
          response.close();
        } catch (IOException e) {
          throw new StorageException("Couldn't close response after DELETE item request", e);
        }
      }
    }
  }

  private String getLocationId(JSONObject holdingsRecord) {
    if (holdingsRecord != null) {
      return (String) holdingsRecord.get("permanentLocationId");
    } else {
      return null;
    }
  }

  private String getInstitutionId(JSONObject holdingsRecord) {
    if (holdingsRecord != null) {
      String locationId = getLocationId(holdingsRecord);
      if (locationId != null) {
        return locInstMap.get(locationId);
      }
    }
    return null;
  }

  /**
   * Iterate holdings records and items of the instanceWithHoldingsAndItems object,
   * and POST them to Inventory
   * @param holdingsRecords
   * @param instanceId
   * @throws InventoryUpdateException
   */
  private void addHoldingsRecordsAndItems(JSONArray holdingsRecords, String instanceId) throws InventoryUpdateException {
    if (holdingsRecords != null) {
      Iterator<JSONObject> holdingsrecords = holdingsRecords.iterator();
      while (holdingsrecords.hasNext()) {
        JSONObject holdingsRecord;
        Object holdingsObject = holdingsrecords.next();
        holdingsRecord = (JSONObject) holdingsObject;
        holdingsRecord.put("instanceId", instanceId);
        if (holdingsRecord.containsKey("items")) {
          JSONArray items = null;
          try {
            items = extractJsonArrayFromObject(holdingsRecord, "items");
          } catch (ParseException e) {
            RecordError error = new ExceptionRecordError(e, "Exception when trying to extract item array from holdings record JSON ", "holdings/items");
            recordWithErrors.reportAndThrowError(error, Level.ERROR, e);
          }
          try {
            JSONObject holdingsRecordResponse = addHoldingsRecord(holdingsRecord);
            if (holdingsRecordResponse != null && holdingsRecordResponse.get("id") != null) {
              Iterator<JSONObject> itemsIterator = items.iterator();
              while (itemsIterator.hasNext()) {
                JSONObject item = (JSONObject) itemsIterator.next();
                item.put("holdingsRecordId", holdingsRecordResponse.get("id").toString());
                try {
                  addItem(item);
                } catch (InventoryUpdateException iue) {
                  // item update error was logged, continue with next item if any
                }
              }
            }
          } catch (InventoryUpdateException iue) {
            // holdings update error was logged, continue with next holdings record if any
          }
        } else {
          try {
            addHoldingsRecord(holdingsRecord);
          } catch (InventoryUpdateException iue) {
            // holdings update error was logged, continue with next holdings record if any
          }
        }
      }
    } else {
      logger.warn("Inventory record storage found empty list of holdings records in instance input");
    }
  }

  /**
   * POST item to Inventory
   * @param item
   * @return Inventory's response to the POST request
   * @throws InventoryUpdateException
   */
  private JSONObject addItem(JSONObject item) throws InventoryUpdateException {
    updateCounters.itemsProcessed++;
    HttpPost httpPost = new HttpPost(ctxt.itemStorageUrl);
    StringEntity entity = new StringEntity(item.toJSONString(), "UTF-8");
    httpPost.setEntity(entity);
    setHeaders(httpPost,"application/json");
    JSONObject itemResponse = null;
    try {
      CloseableHttpResponse response = ctxt.inventoryClient.execute(httpPost);
      String responseAsString = EntityUtils.toString(response.getEntity());
      itemResponse = getResponseAsJson(responseAsString);
      response.close();
      if (response.getStatusLine().getStatusCode() != 201) {
        updateCounters.itemsFailed++;
        String responseStr = response.getEntity().toString();
        RecordError error = new HttpRecordError(response.getStatusLine(), responseStr, responseStr, "Error adding item record ", "item");
        recordWithErrors.reportAndThrowError(error, Level.DEBUG);
      } else {
        updateCounters.itemsLoaded++;
      }
    } catch (IOException | org.apache.http.ParseException e ) {
      updateCounters.itemsFailed++;
      RecordError error = new ExceptionRecordError(e, "Error adding item record", "item");
      recordWithErrors.reportAndThrowError(error, Level.DEBUG);
    }
    return itemResponse;
  }

  /**
   * Get items for a holdings record
   * @param holdingsRecordId
   * @return Array of item objects
   * @throws InventoryUpdateException
   */
  private JSONArray getItemsByHoldingsRecordId(String holdingsRecordId) throws InventoryUpdateException {
    String url = String.format("%s?limit=1000&query=holdingsRecordId%%3D%%3D%s", ctxt.itemStorageUrl, holdingsRecordId);
    HttpGet httpGet = new HttpGet(url);
    setHeaders(httpGet,"application/json");
    CloseableHttpResponse response = null;
    try {
      response = ctxt.inventoryClient.execute(httpGet);
      if (!Arrays.asList(200, 404).contains(response.getStatusLine().getStatusCode())) {
        RecordError error = new HttpRecordError(response.getStatusLine(), response.getEntity().toString(),
                                          response.getEntity().toString(),
                                          "Error retrieving items for holdingsRecord ", "items");
        recordWithErrors.reportAndThrowError(error, Level.DEBUG);
      }
    } catch (IOException ioe) {
      RecordError error = new ExceptionRecordError(ioe, "Exception when GETing items by holdings ID ", "item");
      recordWithErrors.reportAndThrowError(error, Level.ERROR, ioe);
    }
    JSONArray itemsJson = null;
    try {
      JSONObject jsonResponse;
      JSONParser parser = new JSONParser();
      jsonResponse = (JSONObject) parser.parse(EntityUtils.toString(response.getEntity()));
      itemsJson = (JSONArray) (jsonResponse.get("items"));
    } catch (ParseException | IOException e) {
      RecordError error = new ExceptionRecordError(e, "Exception when reading response for GET items by holdings ID", "item");
      recordWithErrors.reportAndThrowError(error, Level.ERROR, e);
    }
    return itemsJson;
  }

  /**
   * Delete a holdings record by ID
   * @param uuid
   * @throws InventoryUpdateException
   */
  private void deleteHoldingsRecord(String uuid) throws InventoryUpdateException {
    logger.log(Level.TRACE,"Deleting holdingsRecord with ID: " + uuid);
    String url = String.format("%s/%s", ctxt.holdingsStorageUrl, uuid);
    HttpDelete httpDelete = new HttpDelete(url);
    setHeaders(httpDelete,"text/plain");
    CloseableHttpResponse response = null;
    try {
      response = ctxt.inventoryClient.execute(httpDelete);
      if (response.getStatusLine().getStatusCode() != 204) {
        RecordError error = new HttpRecordError(response.getStatusLine(), response.getEntity().toString(), response.getEntity().toString(), "Error deleting holdingsRecord ", "holdingsRecord");
        recordWithErrors.reportAndThrowError(error, Level.ERROR);
      }
    } catch (IOException io) {
      RecordError error = new ExceptionRecordError(io, "Exception when deleting holdingsRecord", "holdingsRecord");
      recordWithErrors.reportAndThrowError(error, Level.ERROR, io);
    } finally {
      if (response != null) {
        try {
          response.close();
        } catch (IOException ioe) {
          throw new StorageException("Could not close response when trying to delete holdings record", ioe);
        }
      }
    }
  }

  /**
   * Create JSONObject from the XML in the incoming record
   * @param record
   * @return
   * @throws InventoryUpdateException
   */
  private JSONObject getMarcJson(TransformedRecord record) throws InventoryUpdateException {
    JSONObject marcJson = null;
    if (record.getOriginalContent() != null) {
      try {
        logger.log(Level.TRACE,"Incoming original content: " + new String(record.getOriginalContent(), "UTF-8"));
        marcJson = MarcXMLToJson.convertMarcXMLToJson(new String(record.getOriginalContent(), "UTF-8"));
        logger.log(Level.TRACE, "Original content converted to JSON: " + marcJson.toJSONString());
      } catch (IOException | ParserConfigurationException | SAXException e) {
        updateCounters.sourceRecordsFailed++;
        RecordError error = new ExceptionRecordError(e, "Error creating MARC JSON for source record", "MARC source");
        recordWithErrors.reportAndThrowError(error, Level.DEBUG);
      }
    } else {
      if (ctxt.harvestable.isStoreOriginal()) {
        updateCounters.sourceRecordsFailed++;
        RecordError error = new ExceptionRecordError(new InventoryUpdateException("Job set to store original source but no original content found."),
                                              "Job set to store original source but no original content found.",
                                              "MARC source");
        recordWithErrors.reportAndThrowError(error, Level.DEBUG);
      }
    }
    return marcJson;
  }


  /**
   * Gets an instance from FOLIO Inventory by identifier (type and value)
   * @param localIdentifier  Identifier value
   * @param identifierTypeId  Inventory identifier type ID
   * @return Instance JSON response from FOLIO Inventory
   * @throws InventoryUpdateException
   */
  private JSONObject getInstance(String localIdentifier, String identifierTypeId) throws InventoryUpdateException {
    String url = String.format("%s?query=%%28identifiers%%20%%3D%%2F%%40value%%2F%%40identifierTypeId%%3D%%22%s%%22%%20%%22%s%%22%%29",
                                ctxt.folioAddress + "instance-storage/instances", identifierTypeId, localIdentifier);
    HttpGet httpGet = new HttpGet(url);
    setHeaders(httpGet,"application/json");

    CloseableHttpResponse response;
    try {
      response = ctxt.inventoryClient.execute(httpGet);
    } catch (IOException ioe) {
      throw new InventoryUpdateException("IOException when trying to GET Instance by local identifier and identifierTypeId "+ ioe.getMessage(), ioe);
    }
    if (!Arrays.asList(200, 404).contains(response.getStatusLine().getStatusCode())) {
      RecordError error = new HttpRecordError(
        response.getStatusLine(), response.getEntity().toString(),
        response.getEntity().toString(),
        String.format("Error retrieving instance by local identifier %s and identifierTypeId %s", localIdentifier, identifierTypeId),
        "instance");
      recordWithErrors.reportAndThrowError(error, Level.DEBUG);
    }
    JSONObject jsonResponse = null;
    JSONParser parser = new JSONParser();
    String responseString = null;
    try {
      responseString = EntityUtils.toString(response.getEntity());
    } catch (IOException e) {
      RecordError error = new ExceptionRecordError(e, "Exception when reading response for GET Instance ", "instance");
      recordWithErrors.reportAndThrowError(error, Level.ERROR, e);
    }
    logger.debug("getInstanceId response: " + responseString);
    try {
      jsonResponse = (JSONObject) parser.parse(responseString);
    } catch (ParseException e) {
      RecordError error = new ExceptionRecordError(e, "Exception when trying to Instance response as JSON ", "instance");
      recordWithErrors.reportAndThrowError(error, Level.ERROR, e);
    }
    Long totalRecords = (Long) jsonResponse.getOrDefault("totalRecords", 0);
    if (totalRecords == 1 && jsonResponse.get("instances") != null) {
      return (JSONObject) ((JSONArray) jsonResponse.get("instances")).get(0);
    } else {
      logger.debug("totalRecords for instance query by identifier was " + totalRecords);
      return null;
    }
  }

  /**
   * Looks for existing MARC record in FOLIO by uniquely identifying criteria
   * @param instanceId
   * @param institutionId
   * @param localIdentifier
   * @return
   * @throws InventoryUpdateException
   */
  private JSONObject getExistingMarcRecord(String instanceId, String institutionId, String localIdentifier) throws InventoryUpdateException {
    long startGetExistingMarc = System.currentTimeMillis();
    JSONObject marcRecord = null;
    try {
      StringBuilder query = new StringBuilder().append("(instanceId==\"").append(instanceId).append("\"").append(" and institutionId==\"").append(institutionId).append("\"").append(" and localIdentifier==\"").append(localIdentifier).append("\")");
      StringBuilder url;
      url = new StringBuilder().append(ctxt.marcStorageUrl).append("?query=").append(URLEncoder.encode(query.toString(), "UTF-8"));
      HttpGet httpGet = new HttpGet(url.toString());
      setHeaders(httpGet,"application/json");
      CloseableHttpResponse response = ctxt.inventoryClient.execute(httpGet);
      if (response.getStatusLine().getStatusCode() != 200) {
        RecordError error = new HttpRecordError(response.getStatusLine(), response.getEntity().toString(), response.getEntity().toString(), "Error looking up existing MARC record, expected status 200", "MARC source");
        recordWithErrors.reportAndThrowError(error, Level.DEBUG);
      } else {
        String responseAsString = EntityUtils.toString(response.getEntity());
        JSONObject marcRecords = (JSONObject) (new JSONParser().parse(responseAsString));
        final Long count = (Long) (marcRecords.getOrDefault("totalRecords", 0));
        if (count == 0) {
          logger.debug("No MARC source record found for instance [" + instanceId + "], institution [" + institutionId + "] and local identifier [" + localIdentifier + "]");
        } else if (count == 1) {
          logger.debug("Found existing MARC source record for instance [" + instanceId + "], institution [" + institutionId + "] and local identifier [" + localIdentifier + "]");
          JSONArray records = (JSONArray) marcRecords.get("marcrecords");
          marcRecord = (JSONObject) records.get(0);
        } else {
          logger.error("Expected zero or one MARC source records for instance [" + instanceId + "], institution [" + institutionId + "] and local identifier [" + localIdentifier + "] but count was " + count);
        }
      }
      response.close();
    } catch (IOException | ParseException e) {
      RecordError error = new ExceptionRecordError(e, "Error when checking for previously existing MARC record","MARC source");
      recordWithErrors.reportAndThrowError(error, Level.DEBUG);
    }
    logger.debug("Got existing MARC in: " + (System.currentTimeMillis()-startGetExistingMarc));
    return marcRecord;
  }

  /**
   *
   * @param marcJson
   * @param instanceId
   * @param institutionId
   * @param localIdentifier
   * @return
   * @throws InventoryUpdateException
   */
  private JSONObject addOrUpdateMarcRecord(JSONObject marcJson, String instanceId, String institutionId, String localIdentifier) throws InventoryUpdateException {
    JSONObject marcResponse = null;
    if (marcJson != null) {
      JSONObject marcPostJson = new JSONObject();
      marcPostJson.put("instanceId", instanceId);
      marcPostJson.put("institutionId", institutionId);
      marcPostJson.put("localIdentifier", localIdentifier);
      marcPostJson.put("parsedMarc", marcJson);
      StringEntity entity = new StringEntity(marcPostJson.toJSONString(), "UTF-8");
      try {
        HttpEntityEnclosingRequestBase request;
        JSONObject marcRecord = getExistingMarcRecord(instanceId, institutionId, localIdentifier);
        if (marcRecord == null) {
          logger.debug("This MARC record did not exist in storage; creating it.");
          long startAddingMarc = System.currentTimeMillis();
          request = new HttpPost(ctxt.marcStorageUrl);
          request.setEntity(entity);
          setHeaders(request,"application/json");
          CloseableHttpResponse response = ctxt.inventoryClient.execute(request);
          String responseAsString = EntityUtils.toString(response.getEntity());
          marcResponse = getResponseAsJson(responseAsString);
          if (response.getStatusLine().getStatusCode() != 201) {
            updateCounters.sourceRecordsFailed++;
            RecordError error = new HttpRecordError(response.getStatusLine(), responseAsString, responseAsString, "Error adding MARC source record ", "MARC source");
            recordWithErrors.reportAndThrowError(error, Level.DEBUG);
          } else {
            logger.debug("Status code: " + response.getStatusLine().getStatusCode() + " for POST of marc json " + marcPostJson.toJSONString());
            updateCounters.sourceRecordsLoaded++;
          }
          response.close();
          logger.debug("Added MARC in " + (System.currentTimeMillis()-startAddingMarc));
        } else {
          logger.debug("This MARC record already existed in storage; updating it.");
          long startUpdatingMarc = System.currentTimeMillis();
          String id = (String) marcRecord.get("id");
          String url = ctxt.marcStorageUrl + "/" + id;
          request = new HttpPut(url);
          request.setEntity(entity);
          setHeaders(request,"text/plain");
          CloseableHttpResponse response = ctxt.inventoryClient.execute(request);
          response.close();
          if (response.getStatusLine().getStatusCode() != 204) {
            updateCounters.sourceRecordsFailed++;
            RecordError error = new HttpRecordError(response.getStatusLine(), response.getEntity().toString(), response.getEntity().toString(), "Error updating existing MARC record", "MARC source");
            recordWithErrors.reportAndThrowError(error, Level.DEBUG);
          } else {
            logger.debug("Status code: " + response.getStatusLine().getStatusCode() + " for PUT of marc json " + marcPostJson.toJSONString());
            updateCounters.sourceRecordsLoaded++;
          }
          logger.debug("Updated MARC in " + (System.currentTimeMillis()-startUpdatingMarc));
        }
      } catch (IOException | org.apache.http.ParseException | UnsupportedCharsetException e) {
        updateCounters.sourceRecordsFailed++;
        RecordError error = new ExceptionRecordError(e, "Error adding MARC source record", "MARC source");
        recordWithErrors.reportAndThrowError(error, Level.DEBUG);
      }
    }
    return marcResponse;
  }

  /**
   * Delete a source record by ID
   * @param uuid
   * @throws InventoryUpdateException
   */
  private void deleteSourceRecord(String uuid) throws InventoryUpdateException {
    logger.log(Level.TRACE,"Deleting source record with ID: " + uuid);
    String url = String.format("%s/%s", ctxt.marcStorageUrl, uuid);
    HttpDelete httpDelete = new HttpDelete(url);
    setHeaders(httpDelete,"text/plain");
    CloseableHttpResponse response = null;
    try {
      response = ctxt.inventoryClient.execute(httpDelete);
      if (response.getStatusLine().getStatusCode() != 204) {
        RecordError error = new HttpRecordError(response.getStatusLine(), response.getEntity().toString(), response.getEntity().toString(), "Error deleting source record", "MARC source");
        recordWithErrors.reportAndThrowError(error, Level.DEBUG);
      } else {
        updateCounters.sourceRecordsDeleted++;
      }
    } catch (IOException e) {
      RecordError error = new ExceptionRecordError(e, "Error DELETEing MARC source record", "MARC source");
      recordWithErrors.reportAndThrowError(error, Level.DEBUG, e);
    } finally {
      if (response != null) {
        try {
          response.close();
        } catch (IOException e) {
          throw new StorageException("Couldn't close response after DELETE source record request", e);
        }

      }
    }
  }

  /**
   * Handles delete signal from the transformation pipeline according to Shared Index (ReShare) requirements
   * @param recordJSON contains deletion information
   */
  public void delete(RecordJSON recordJSON) {
    try {
      updateCounters.instanceDeleteSignals++;
      TransformedRecord transformedRecord = new TransformedRecord(recordJSON, logger);
      this.recordWithErrors = new RecordWithErrors(transformedRecord, failedRecordsController);
      if (transformedRecord.isDeleted()) {
        logger.log(Level.TRACE, "Delete request received: " + transformedRecord.getDelete().toJSONString());
        JSONObject deletionJson = transformedRecord.getDelete();
        String oaiId = (String) deletionJson.get("oaiIdentifier");
        String localIdentifier = (oaiId != null ? oaiId.substring(oaiId.lastIndexOf(":")+1) : null);
        String identifierTypeId = (String) deletionJson.get("identifierTypeId");
        String institutionId = (String) deletionJson.get("institutionId");
        if (localIdentifier != null && identifierTypeId != null && institutionId != null) {
          // This is assumed to be a deletion record targeted for a shared inventory
          logger.debug("Storage class received a deletion record with local identifier: [" + localIdentifier +"], identifierTypeId ["+identifierTypeId+"], institutionId ["+institutionId+"]");
          JSONObject instance;
          instance = getInstance(localIdentifier, identifierTypeId);
          if (instance != null) {
            String instanceId = (String) instance.get("id");
            logger.debug("Found instance to 'delete' [" + instanceId + "]");
            removeIdentifierFromInstanceForInstitution(localIdentifier, identifierTypeId, instance);
            deleteHoldingsAndItemsForInstitution(instanceId, institutionId, true);
            updateInstance(instance);
            if (ctxt.marcStorageUrlIsDefined) {
              deleteMarcSourceRecordForInstitution(localIdentifier, institutionId, instanceId);
            }
            updateCounters.instanceDeletions++;
            ((InventoryStorageStatus) ctxt.storageStatus).incrementDelete(1);
          } else {
            logger.info("Received delete signal but no existing instance found with local id ["+localIdentifier+"] and identifierTypeId ["+identifierTypeId+"]. No deletion performed.");
          }
        } else if (localIdentifier != null) {
          // This is assumed to be a deletion record targeted for a simple inventory
          logger.info("Storage class received a deletion record with ID: [" + localIdentifier +"]");
        } else if (oaiId != null && localIdentifier == null) {
          logger.error("ID not found in the OAI identifier [" + oaiId + "]. Cannot perform delete against Inventory");
        } else if (oaiId == null) {
          logger.error("No OAI identifier found in deletion record. Cannot perform delete against Inventory");
        }
      } else {
        logger.error("Inventory storage class received delete request but didn't recognize the payload as a delete: " + transformedRecord.toString());
      }
      if (recordWithErrors.hasErrors()) {
        recordWithErrors.writeErrorsLog(logger);
      }
    } catch (InventoryUpdateException iue) {
      recordWithErrors.writeErrorsLog(logger);
    }
  }

  /**
   * Shared Index (ReShare) logic for removing underlying MARC source record, if any, that was received
   * from the institution now sending a delete request.
   * @param localIdentifier
   * @param institutionId
   * @param instanceId
   * @throws InventoryUpdateException
   */
  private void deleteMarcSourceRecordForInstitution(String localIdentifier, String institutionId, String instanceId) throws InventoryUpdateException {
    JSONObject marcRecord = getExistingMarcRecord(instanceId, institutionId, localIdentifier);
    if (marcRecord != null) {
      String sourceId = (String) marcRecord.get("id");
      deleteSourceRecord(sourceId);
    } else {
      logger.log(Level.DEBUG,"Found no source record to delete for instance [" + instanceId + "], institution [" + institutionId + "] and local identifier [" + localIdentifier + "]");
    }
  }

  /**
   * Shared Index (ReShare) logic for updating the Instance on delete requests: Remove a given library's record identifier from the
   * shared master Instance. Mutates the `instance` argument
   * @param localIdentifier
   * @param identifierTypeId
   * @param instance
   */
  private void removeIdentifierFromInstanceForInstitution(String localIdentifier, String identifierTypeId, JSONObject instance) {
    JSONArray identifiers = (JSONArray)instance.get("identifiers");
    JSONObject identifier = null;
    Iterator<JSONObject> iter = identifiers.iterator();
    while (iter.hasNext()) {
      JSONObject identifierObject = (JSONObject) iter.next();
      if (identifierTypeId.equals(identifierObject.get("identifierTypeId"))
         && localIdentifier.equals(identifierObject.get("value"))) {
        identifier = identifierObject;
        break;
      }
    }
    identifiers.remove(identifier);
    logger.debug("Removed " + identifier.toJSONString() + " from " + instance.toJSONString());
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

  /**
   * Convenience method with the most common Inventory request headers
   * @param request
   * @param accept
   */
  private void setHeaders (HttpRequestBase request, String accept) {
    request.setHeader("Accept", accept);
    request.setHeader("Content-type", "application/json");
    request.setHeader("X-Okapi-Token", ctxt.authToken);
    request.setHeader("X-Okapi-Tenant", ctxt.folioTenant);
  }

  @SuppressWarnings("unused")
  private void deleteInstanceRecord(CloseableHttpClient client, String id, String tenant, String authToken) throws InventoryUpdateException {
    String url = String.format("%s/instances/%s", ctxt.folioAddress + ctxt.instanceStoragePath, id);
    HttpDelete httpDelete = new HttpDelete(url);
    setHeaders(httpDelete,"application/json");
    CloseableHttpResponse response;
    try {
      response = client.execute(httpDelete);
    } catch (IOException e) {
      throw new InventoryUpdateException("IOException when trying to delete Instance record: " + e.getMessage(), e);
    }
    if (response.getStatusLine().getStatusCode() != 204) {
      RecordError error = new HttpRecordError(response.getStatusLine(), response.getEntity().toString(), response.getEntity().toString(), "Error deleting instance record", "Instance");
      recordWithErrors.reportAndThrowError(error, Level.DEBUG);
    }
  }

}
