/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import com.indexdata.masterkey.localindices.util.MarcXMLToJson;

/**
 * Logic for handling the create/update/delete for one FOLIO Inventory record
 * set -- an Inventory record set being understood as an instance with
 * holdings/items and possibly a source record, all created from one
 * incoming bibliographic record.
 *
 * @author ne
 */
public class InventoryRecordUpdater {

  private final InventoryStorageController ctrl;
  private final StorageJobLogger logger;
  private RecordErrors errors;
  private final RecordUpdateCounts counts;
  private final Map<String, String> locInstMap;

  public InventoryRecordUpdater (InventoryStorageController ctrl) {
    this.ctrl = ctrl;
    logger = ctrl.logger;
    counts = ctrl.counters;
    locInstMap = ctrl.locationsToInstitutionsMap;
  }

  void addInventory(RecordJSON recordJson) {
    this.errors = new RecordErrors(recordJson);
    long startStorageEntireRecord = System.currentTimeMillis();
    TransformedRecord transformedRecord = new TransformedRecord(recordJson.toJson(), logger);
    JSONObject instanceResponse;
    if (ctrl.getConfigurationValue(InventoryStorageController.INSTANCE_STORAGE_PATH).contains("match")) {
      instanceResponse = addInstanceRecord(transformedRecord.getInstance(), transformedRecord.getMatchKey());
    } else {
      instanceResponse = addInstanceRecord(transformedRecord.getInstance());
    }
    String institutionId = transformedRecord.getInstitutionId(locInstMap);
    String localIdentifier = transformedRecord.getLocalIdentifier();
    if (instanceResponse != null && instanceResponse.get("id") != null) {
      JSONArray holdingsJson = transformedRecord.getHoldings();
      if (holdingsJson != null && holdingsJson.size() > 0) {
        String instanceId = instanceResponse.get("id").toString();
        // delete existing holdings/items from the same institution
        // before attaching new holdings/items to the instance
        try {
          deleteHoldingsAndItemsForInstitution(instanceId, institutionId, false);
          addHoldingsRecordsAndItems(holdingsJson, instanceId);
        } catch (ParseException | ClassCastException | IOException e) {
          String msg = "Error adding holdings record and/or items: " + e.getLocalizedMessage();
          logger.error(msg);
          errors.addErrorMessage(msg);
          counts.holdingsRecordsFailed++;
        }
      }
      if (ctrl.harvestable.isStoreOriginal()) {
        counts.sourceRecordsProcessed++;
        JSONObject marcJson = getMarcJson((RecordJSON) recordJson);
        addOrUpdateMarcRecord(marcJson, (String) instanceResponse.get("id"), institutionId, localIdentifier);
      }
      ctrl.timingsEntireRecord.time(startStorageEntireRecord);
      if (counts.instancesLoaded % (counts.instancesLoaded < 1000 ? 100 : 1000) == 0) {
        logger.info("" + counts.instancesLoaded + " instances, " + counts.holdingsRecordsLoaded + " holdings records, " + counts.itemsLoaded + " items, and " + counts.sourceRecordsLoaded + " source records ingested.");
        if (counts.instancesFailed + counts.holdingsRecordsFailed + counts.itemsFailed > 0) {
          logger.info("Failed: " + counts.instancesFailed + " instances, " + counts.holdingsRecordsFailed + " holdings records, " + counts.itemsFailed + " items, and " + counts.sourceRecordsFailed + " source records.");
        }
      }
    } else {
      if (recordJson.isDeleted()) {
        logger.info("Record is deleted: [" + recordJson.getId() + "], [" + ((RecordJSON) recordJson).toJson() + "]");
      } else {
        logger.info("Inventory record storage received instance record that was not a delete but also with no 'title' property, [" + ((RecordJSON) recordJson).toJson() + "] cannot create in Inventory, skipping. ");
      }
    }
  }


  private JSONObject addInstanceRecord(JSONObject instanceRecord) {
    JSONObject noMatchKey = new JSONObject();
    return addInstanceRecord(instanceRecord, noMatchKey);
  }

  /**
   * POST instance to Inventory
   * @param instanceRecord
   * @return
   * @throws UnsupportedEncodingException
   * @throws IOException
   * @throws ParseException
   */
  private JSONObject addInstanceRecord(JSONObject instanceRecord, JSONObject matchKey) {
    JSONObject instanceResponse = null;
    counts.instancesProcessed++;
    try {
      String url = ctrl.folioAddress + ctrl.getConfigurationValue(InventoryStorageController.INSTANCE_STORAGE_PATH);
      HttpEntityEnclosingRequestBase httpUpdate;
      if (url.contains("instance-storage-match")) {
        if (!matchKey.isEmpty()) {
          instanceRecord.put("matchKey", matchKey);
        }
        httpUpdate = new HttpPut(url);
      } else {
        httpUpdate = new HttpPost(url);
      }
      StringEntity entity = new StringEntity(instanceRecord.toJSONString(), "UTF-8");
      httpUpdate.setEntity(entity);
      setHeaders(httpUpdate,"application/json");
      CloseableHttpResponse response = ctrl.client.execute(httpUpdate);
      JSONParser parser = new JSONParser();
      String responseAsString = EntityUtils.toString(response.getEntity());
      try {
        instanceResponse = (JSONObject) parser.parse(responseAsString);
      } catch (ParseException pe) {
        instanceResponse = new JSONObject();
        instanceResponse.put("wrappedErrorMessage", responseAsString);
      }
      response.close();
      if (response.getStatusLine().getStatusCode() != 201 && response.getStatusLine().getStatusCode() != 200) {
        counts.instancesFailed++;
        String errorMessage = response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase();
        errors.addErrorMessage(errorMessage);
        if (counts.instanceExceptionCounts.containsKey(errorMessage)) {
          counts.instanceExceptionCounts.put(errorMessage, counts.instanceExceptionCounts.get(errorMessage) + 1);
        } else {
          counts.instanceExceptionCounts.put(errorMessage, 1);
        }
        if (counts.instanceExceptionCounts.get(errorMessage) < 10 || counts.instanceExceptionCounts.get(errorMessage) % 100 == 0) {
          logger.error(String.format("%d instances failed with %s", counts.instanceExceptionCounts.get(errorMessage), errorMessage));
        }
        logger.debug(String.format("Got error %s, %s adding Instance record: %s", response.getStatusLine().getStatusCode(), responseAsString, instanceRecord.toJSONString()));
      } else {
        ((InventoryStorageStatus) ctrl.storageStatus).incrementAdd(1);
        counts.instancesLoaded++;
      }
    } catch (IOException | org.apache.http.ParseException e) {
      counts.instancesFailed++;
      String errorMessage = String.format("Got error adding Instance record: %s", e.getLocalizedMessage());
      errors.addErrorMessage(errorMessage);
      if (counts.instanceExceptionCounts.containsKey(errorMessage)) {
        counts.instanceExceptionCounts.put(errorMessage, counts.instanceExceptionCounts.get(errorMessage) + 1);
      } else {
        counts.instanceExceptionCounts.put(errorMessage, 1);
      }
      if (counts.instanceExceptionCounts.get(errorMessage) < 10 || counts.instanceExceptionCounts.get(errorMessage) % 100 == 0) {
        logger.error(String.format("%d instances failed with %s", counts.instanceExceptionCounts.get(errorMessage), errorMessage));
      }
      logger.debug("Error storing Instance record: %s " + e.getLocalizedMessage());
    }
    return instanceResponse;
  }

  /**
   * PUTs the instance object argument to Inventory
   * @param instance
   */
  private void updateInstance(JSONObject instance) {
    logger.debug("Updating Instance with " + instance.toJSONString());
    try {
      String url = ctrl.folioAddress + "instance-storage/instances/" + instance.get("id");
      HttpEntityEnclosingRequestBase httpUpdate;
      httpUpdate = new HttpPut(url);
      StringEntity entity = new StringEntity(instance.toJSONString(), "UTF-8");
      httpUpdate.setEntity(entity);
      setHeaders(httpUpdate,"text/plain");
      CloseableHttpResponse response = ctrl.client.execute(httpUpdate);
      response.close();
      logger.debug("Updated instance " + instance.get("id"));
    } catch (IOException ioe) {
      String err = "IO error updating instance: " + ioe.getMessage();
      errors.addErrorMessage(err);
      logger.error(err);
    }
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
    counts.holdingsRecordsProcessed++;
    String url = ctrl.folioAddress + ctrl.getConfigurationValue(InventoryStorageController.HOLDINGS_STORAGE_PATH);
    HttpPost httpPost = new HttpPost(url);
    StringEntity entity = new StringEntity(holdingsRecord.toJSONString(), "UTF-8");
    httpPost.setEntity(entity);
    setHeaders(httpPost,"application/json");
    JSONObject holdingsRecordResponse = null;
    try {
      CloseableHttpResponse response = ctrl.client.execute(httpPost);
      JSONParser parser = new JSONParser();
      String responseAsString = EntityUtils.toString(response.getEntity());
      try {
        holdingsRecordResponse = (JSONObject) parser.parse(responseAsString);
      } catch (ParseException pe) {
        holdingsRecordResponse = new JSONObject();
        holdingsRecordResponse.put("wrappedErrorMessage", responseAsString);
        errors.addErrorMessage(responseAsString);
      }
      response.close();
      if (response.getStatusLine().getStatusCode() != 201) {
        counts.holdingsRecordsFailed++;
        String err = String.format("Got error %s, %s adding holdingsRecord: %s", response.getStatusLine().getStatusCode(), responseAsString, holdingsRecord.toJSONString());
        logger.error(err);
        errors.addErrorMessage(err);
      } else {
        counts.holdingsRecordsLoaded++;
      }
    } catch (IOException | org.apache.http.ParseException e) {
      counts.holdingsRecordsFailed++;
      String err = String.format("Got error adding holdingsRecord: %s", e.getLocalizedMessage());
      logger.error(err);
      errors.addErrorMessage(err);
    }
    return holdingsRecordResponse;
  }

  private JSONObject getExistingMarcRecord(String instanceId, String institutionId, String localIdentifier) {
    JSONObject marcRecord = null;
    try {
      StringBuilder query = new StringBuilder().append("(instanceId==\"").append(instanceId).append("\"").append(" and institutionId==\"").append(institutionId).append("\"").append(" and localIdentifier==\"").append(localIdentifier).append("\")");
      StringBuilder url;
      url = new StringBuilder().append(ctrl.folioAddress).append("/marc-records?query=").append(URLEncoder.encode(query.toString(), "UTF-8"));
      HttpGet httpGet = new HttpGet(url.toString());
      setHeaders(httpGet,"application/json");
      CloseableHttpResponse response = ctrl.client.execute(httpGet);
      if (response.getStatusLine().getStatusCode() != 200) {
        String err = "Error looking up existing MARC record, expected status 200, got " + response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase();
        logger.error(err);
        errors.addErrorMessage(err);
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
          logger.error("Expected zero or on MARC source records for instance [" + instanceId + "], institution [" + institutionId + "] and local identifier [" + localIdentifier + "] but count was " + count);
        }
      }
      response.close();
    } catch (IOException | ParseException e) {
      String err = "Error when checking for previously existing MARC record: " + e.getMessage();
      logger.error(err);
      errors.addErrorMessage(err);
    }
    return marcRecord;
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

  private JSONObject addOrUpdateMarcRecord(JSONObject marcJson, String instanceId, String institutionId, String localIdentifier) {
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
          String url = ctrl.folioAddress + "/marc-records"; //TODO: Add configuration value
          request = new HttpPost(url);
          request.setEntity(entity);
          setHeaders(request,"application/json");
          CloseableHttpResponse response = ctrl.client.execute(request);
          JSONParser parser = new JSONParser();
          String responseAsString = EntityUtils.toString(response.getEntity());
          try {
            marcResponse = (JSONObject) parser.parse(responseAsString);
          } catch (ParseException pe) {
            marcResponse = new JSONObject();
            marcResponse.put("wrappedErrorMessage", responseAsString);
          }
          response.close();
          if (response.getStatusLine().getStatusCode() != 201) {
            String err = String.format("Got error %s, %s adding record: %s", response.getStatusLine().getStatusCode(), responseAsString, marcPostJson.toJSONString());
            logger.error(err);
            errors.addErrorMessage(err);
            counts.sourceRecordsFailed++;
          } else {
            logger.debug("Status code: " + response.getStatusLine().getStatusCode() + " for POST of marc json " + marcPostJson.toJSONString());
            counts.sourceRecordsLoaded++;
          }
        } else {
          logger.debug("This MARC record already existed in storage; updating it.");
          String id = (String) marcRecord.get("id");
          String url = ctrl.folioAddress + "/marc-records/" + id; //TODO: Add configuration value
          request = new HttpPut(url);
          request.setEntity(entity);
          setHeaders(request,"text/plain");
          CloseableHttpResponse response = ctrl.client.execute(request);
          response.close();
          if (response.getStatusLine().getStatusCode() != 204) {
            String err = String.format("Got error %s, %s updating record: %s", response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase(), marcPostJson.toJSONString());
            logger.error(err);
            errors.addErrorMessage(err);
            counts.sourceRecordsFailed++;
          } else {
            logger.debug("Status code: " + response.getStatusLine().getStatusCode() + " for PUT of marc json " + marcPostJson.toJSONString());
            counts.sourceRecordsLoaded++;
          }
        }
      } catch (IOException | org.apache.http.ParseException | UnsupportedCharsetException e) {
        String err = "Error adding MARC source record: " + e.getLocalizedMessage();
        logger.error(err);
        errors.addErrorMessage(err);
        counts.sourceRecordsFailed++;
      }
    }
    return marcResponse;
  }

  /**
   * Delete a source record by ID
   * @param uuid
   * @throws IOException
   */
  private void deleteSourceRecord(String uuid) throws IOException {
    logger.debug("Deleting source record with ID: " + uuid);
    String url = String.format("%s/%s", ctrl.folioAddress + "/marc-records/", uuid);
    HttpDelete httpDelete = new HttpDelete(url);
    setHeaders(httpDelete,"text/plain");
    CloseableHttpResponse response = null;
    try {
      response = ctrl.client.execute(httpDelete);
      if (response.getStatusLine().getStatusCode() != 204) {
        String err = String.format("Got error deleting source record with id '%s': %s", uuid, EntityUtils.toString(response.getEntity()));
        errors.addErrorMessage(err);
        throw new IOException(err);
      } else {
        counts.sourceRecordsDeleted++;
      }
    } finally {
      if (response != null) {
        response.close();
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
  private JSONArray getHoldingsRecordsByInstanceId(String instanceId) throws IOException, ParseException {
    String url = String.format("%s?limit=1000&query=instanceId%%3D%%3D%s", ctrl.folioAddress + ctrl.getConfigurationValue(InventoryStorageController.HOLDINGS_STORAGE_PATH), instanceId);
    HttpGet httpGet = new HttpGet(url);
    setHeaders(httpGet,"application/json");
    CloseableHttpResponse response = ctrl.client.execute(httpGet);
    if (!Arrays.asList(200, 404).contains(response.getStatusLine().getStatusCode())) {
      String err = String.format("Got error retrieving holdings records for instance with id '%s': %s", instanceId, EntityUtils.toString(response.getEntity()));
      throw new IOException(err);
    }
    JSONObject jsonResponse;
    JSONParser parser = new JSONParser();
    String responseString = EntityUtils.toString(response.getEntity());
    jsonResponse = (JSONObject) parser.parse(responseString);
    JSONArray holdingsRecordsJson = (JSONArray) (jsonResponse.get("holdingsRecords"));
    return holdingsRecordsJson;
  }

  /**
   * Wipes out existing holdings and items belonging to the institution, for the given instance
   * @param instanceId
   * @throws IOException
   * @throws ParseException
   */
  private void deleteHoldingsAndItemsForInstitution(String instanceId, String institutionId, boolean countDeletions) throws IOException, ParseException {
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
                if (countDeletions) {
                  counts.itemsDeleted++;
                }
              }
            }
            try {
              deleteHoldingsRecord(existingHoldingsRecordId);
              if (countDeletions) {
                counts.holdingsRecordsDeleted++;
              }
            } catch (IOException ioe) {
              if (ioe.getMessage().contains("still referenced")) {
                String err = "Holdings record for deletion: " + existingHoldingsRecord.toJSONString() + " had " + itemsToDelete + " items.";
                logger.info(err);
                errors.addErrorMessage(err);
                err = "Items referencing the holdings record: " + getItemsByHoldingsRecordId(existingHoldingsRecordId).toJSONString();
                logger.info(err);
                errors.addErrorMessage(err);
              }
              throw ioe;
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
   * @throws IOException
   */
  private void deleteItem(String uuid) throws IOException {
    logger.debug("Deleting item with ID: " + uuid);
    String url = String.format("%s/%s", ctrl.folioAddress + ctrl.getConfigurationValue(InventoryStorageController.ITEM_STORAGE_PATH), uuid);
    HttpDelete httpDelete = new HttpDelete(url);
    setHeaders(httpDelete,"text/plain");
    CloseableHttpResponse response = null;
    try {
      response = ctrl.client.execute(httpDelete);
      if (response.getStatusLine().getStatusCode() != 204) {
        String err = String.format("Got error deleting item with id '%s': %s", uuid, EntityUtils.toString(response.getEntity()));
        errors.addErrorMessage(err);
        throw new IOException(String.format("Got error deleting item with id '%s': %s", uuid, EntityUtils.toString(response.getEntity())));
      }
    } finally {
      if (response != null) {
        response.close();
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

  /**
   * Iterate holdings records and items of the instanceWithHoldingsAndItems object,
   * and POST them to Inventory
   * @param holdingsRecords
   * @param instanceId
   * @throws ParseException
   * @throws UnsupportedEncodingException
   * @throws IOException
   */
  private void addHoldingsRecordsAndItems(JSONArray holdingsRecords, String instanceId) throws ParseException, UnsupportedEncodingException, IOException {
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
          String err = "Could not parse holdings record from JSONArray: " + holdingsObject;
          errors.addErrorMessage(err);
          throw new IOException(err);
        }
      }
    } else {
      logger.warn("Inventory record storage found empty list of holdings records in instance input");
    }
  }

  private void deleteInstanceRecord(CloseableHttpClient client, String id, String tenant, String authToken) throws IOException {
    String url = String.format("%s/instances/%s", ctrl.folioAddress + ctrl.getConfigurationValue(InventoryStorageController.INSTANCE_STORAGE_PATH), id);
    HttpDelete httpDelete = new HttpDelete(url);
    setHeaders(httpDelete,"application/json");
    CloseableHttpResponse response = client.execute(httpDelete);
    if (response.getStatusLine().getStatusCode() != 204) {
      String err = String.format("Got error deleting instance record with id '%s': %s", id, EntityUtils.toString(response.getEntity()));
      errors.addErrorMessage(err);
      throw new IOException(err);
    }
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
    counts.itemsProcessed++;
    String url = ctrl.folioAddress + ctrl.getConfigurationValue(InventoryStorageController.ITEM_STORAGE_PATH);
    HttpPost httpPost = new HttpPost(url);
    StringEntity entity = new StringEntity(item.toJSONString(), "UTF-8");
    httpPost.setEntity(entity);
    setHeaders(httpPost,"application/json");
    JSONObject itemResponse = null;
    try {
      CloseableHttpResponse response = ctrl.client.execute(httpPost);
      JSONParser parser = new JSONParser();
      String responseAsString = EntityUtils.toString(response.getEntity());
      try {
        itemResponse = (JSONObject) parser.parse(responseAsString);
      } catch (ParseException pe) {
        itemResponse = new JSONObject();
        itemResponse.put("wrappedErrorMessage", responseAsString);
      }
      response.close();
      if (response.getStatusLine().getStatusCode() != 201) {
        counts.itemsFailed++;
        String err = String.format("Got error %s, %s adding item record: %s", response.getStatusLine().getStatusCode(), responseAsString, item.toJSONString());
        errors.addErrorMessage(err);
        logger.error(err);
      } else {
        counts.itemsLoaded++;
      }
    } catch (IOException | org.apache.http.ParseException e) {
      counts.itemsFailed++;
      String err = String.format("Got error adding item record: %s", e.getLocalizedMessage());
      errors.addErrorMessage(err);
      logger.error(err);
    }
    return itemResponse;
  }

  private JSONObject getMarcJson(RecordJSON record) {
    JSONObject marcJson = null;
    if (record.getOriginalContent() != null) {
      try {
        logger.debug(ctrl.getClass().getSimpleName() + " originalContent to store for Record with a collection of " + record.getSubRecords().size() + " record(s):" + new String(record.getOriginalContent(), "UTF-8"));
        marcJson = MarcXMLToJson.convertMarcXMLToJson(new String(record.getOriginalContent(), "UTF-8"));
        logger.debug(marcJson.toJSONString());
      } catch (IOException | ParserConfigurationException | SAXException e) {
        String err = "Error creating MARC JSON for source record: " + e.getLocalizedMessage();
        logger.error(err);
        errors.addErrorMessage(err);
        counts.sourceRecordsFailed++;
      }
    } else {
      if (ctrl.harvestable.isStoreOriginal()) {
        String err = "Job set to store original source but no original content found.";
        logger.error(err);
        errors.addErrorMessage(err);
        counts.sourceRecordsFailed++;
      }
    }
    return marcJson;
  }


  /**
   * Get items for a holdings record
   * @param holdingsRecordId
   * @return
   * @throws IOException
   * @throws ParseException
   */
  private JSONArray getItemsByHoldingsRecordId(String holdingsRecordId) throws IOException, ParseException {
    String url = String.format("%s?limit=1000&query=holdingsRecordId%%3D%%3D%s", ctrl.folioAddress + ctrl.getConfigurationValue(InventoryStorageController.ITEM_STORAGE_PATH), holdingsRecordId);
    HttpGet httpGet = new HttpGet(url);
    setHeaders(httpGet,"application/json");
    CloseableHttpResponse response = ctrl.client.execute(httpGet);
    if (!Arrays.asList(200, 404).contains(response.getStatusLine().getStatusCode())) {
      String err = String.format("Got error retrieving items for holdingsRecord with id '%s': %s", holdingsRecordId, EntityUtils.toString(response.getEntity()));
      errors.addErrorMessage(err);
      throw new IOException(err);
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
  private void deleteHoldingsRecord(String uuid) throws IOException {
    logger.debug("Deleting holdingsRecord with ID: " + uuid);
    String url = String.format("%s/%s", ctrl.folioAddress + ctrl.getConfigurationValue(InventoryStorageController.HOLDINGS_STORAGE_PATH), uuid);
    HttpDelete httpDelete = new HttpDelete(url);
    setHeaders(httpDelete,"text/plain");
    CloseableHttpResponse response = null;
    try {
      response = ctrl.client.execute(httpDelete);
      if (response.getStatusLine().getStatusCode() != 204) {
        String err = String.format("Got error deleting holdingsRecord with id '%s': %s", uuid, EntityUtils.toString(response.getEntity()));
        errors.addErrorMessage(err);
        throw new IOException(err);
      }
    } finally {
      if (response != null) {
        response.close();
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
  private JSONObject getInstance(String localIdentifier, String identifierTypeId) throws IOException, ParseException {
    String url = String.format("%s?query=%%28identifiers%%20%%3D%%2F%%40value%%2F%%40identifierTypeId%%3D%%22%s%%22%%20%%22%s%%22%%29", ctrl.folioAddress + "instance-storage/instances", identifierTypeId, localIdentifier);
    HttpGet httpGet = new HttpGet(url);
    setHeaders(httpGet,"application/json");
    CloseableHttpResponse response = ctrl.client.execute(httpGet);
    if (!Arrays.asList(200, 404).contains(response.getStatusLine().getStatusCode())) {
      String err = String.format("Got error retrieving instance by local identifier %s and identifierTypeId %s: %s", localIdentifier, identifierTypeId, EntityUtils.toString(response.getEntity()));
      errors.addErrorMessage(err);
      throw new IOException(err);
    }
    JSONObject jsonResponse;
    JSONParser parser = new JSONParser();
    String responseString = EntityUtils.toString(response.getEntity());
    logger.info("getInstanceId response: " + responseString);
    jsonResponse = (JSONObject) parser.parse(responseString);
    Long totalRecords = (Long) jsonResponse.getOrDefault("totalRecords", 0);
    if (totalRecords == 1 && jsonResponse.get("instances") != null) {
      return (JSONObject) ((JSONArray) jsonResponse.get("instances")).get(0);
    } else {
      logger.info("totalRecords for instance query by identifier was " + totalRecords);
      return null;
    }
  }

  public void delete(RecordJSON record) {

    TransformedRecord transformedRecord = new TransformedRecord(record.toJson(), logger);
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
        try {
          JSONObject instance = getInstance(localIdentifier, identifierTypeId);
          if (instance != null) {
            String instanceId = (String) instance.get("id");
            logger.debug("Found instance to 'delete' [" + instanceId + "]");
            JSONArray identifiers = (JSONArray)instance.get("identifiers");
            JSONObject identifier = null;
            Iterator iter = identifiers.iterator();
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
            deleteHoldingsAndItemsForInstitution(instanceId, institutionId, true);
            updateInstance(instance);
            counts.instanceDeletions++;
            JSONObject marcRecord = getExistingMarcRecord(instanceId, institutionId, localIdentifier);
            if (marcRecord != null) {
              String sourceId = (String) marcRecord.get("id");
              deleteSourceRecord(sourceId);
            } else {
              logger.log(Level.DEBUG,"Found no source record to delete for instance [" + instanceId + "], institution [" + institutionId + "] and local identifier [" + localIdentifier + "]");
            }
            ((InventoryStorageStatus) ctrl.storageStatus).incrementDelete(1);
          } else {
            logger.info("No instance found for local id ["+localIdentifier+"] and identifierTypeId ["+identifierTypeId+"]. No deletion performed.");
          }

        } catch (IOException ioe) {
          String err = ioe.getMessage();
          errors.addErrorMessage(err);
          logger.error(err);
        } catch (ParseException pe) {
          logger.error(pe.getMessage());
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

  private void setHeaders (HttpRequestBase request, String accept) {
    request.setHeader("Accept", accept);
    request.setHeader("Content-type", "application/json");
    request.setHeader("X-Okapi-Token", ctrl.authToken);
    request.setHeader("X-Okapi-Tenant", ctrl.getConfigurationValue(InventoryStorageController.FOLIO_TENANT));
  }

}
