package com.indexdata.masterkey.localindices.harvest.storage.folio;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import com.github.cliftonlabs.json_simple.Jsoner;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import com.indexdata.masterkey.localindices.harvest.storage.RecordJSON;
import com.indexdata.masterkey.localindices.harvest.storage.StorageException;
import com.indexdata.masterkey.localindices.util.MarcToJson;
import com.indexdata.masterkey.localindices.util.MarcXMLToJson;

/**
 * Logic for handling the create/update/delete for one FOLIO Inventory record
 * set -- an Inventory record set being an instance with
 * holdings/items and possibly a source record, all created from one
 * incoming bibliographic record.
 *
 * InventoryRecordUpdate gets context from {@link FolioStorageController},
 * for example the job logger and record update/failure counters for logging the
 * process.
 *
 *
 *
 * @author ne
 */
 @SuppressWarnings("unchecked")
 public class InventoryRecordUpdater extends FolioRecordUpdater {

  protected InventoryUpdateContext ctxt;

  /** Overall updates and errors counters */
  private final InventoryRecordUpdateCounters updateCounters;
  private final FailedRecordsController failedRecordsController;
  private final Map<String, String> locInstMap;
  private List<TransformedRecord> batch;

  public InventoryRecordUpdater (InventoryUpdateContext ctxt) {
    this.ctxt = ctxt;
    logger = ctxt.logger;
    failedRecordsController = ctxt.failedRecordsController;
    updateCounters = ctxt.updateCounters;
    locInstMap = ctxt.locationsToInstitutionsMap;
    batch = new ArrayList<>();
  }

  public void addRecord(RecordJSON recordJSON) {
    if (ctxt.batchSize==1) {
      logger.debug("Adding record for single record update " + recordJSON.toJson().toJSONString());
      addSingleRecord(recordJSON);
    } else {
      logger.debug("Adding record for batch update " + recordJSON.toJson().toJSONString());
      addToBatch(recordJSON);
    }
  }


  private void addToBatch(RecordJSON recordJSON) {
    TransformedRecord transformedRecord = new TransformedRecord(recordJSON, logger);
    if (transformedRecord.isRecordExcludedByDateFilter(ctxt)) {
      updateCounters.xmlBulkRecordsSkipped++;
    } else {
      if (!ctxt.inventoryBatchUpsertPath.contains("hrid") || gotAllHrids(transformedRecord)) {
        batch.add(transformedRecord);
        ctxt.timingsCreatingRecord.setTiming(recordJSON.getCreationTiming());
        ctxt.timingsTransformingRecord.setTiming(recordJSON.getTransformationTiming());
      } else {
        logger.error("Record is missing some HRIDs, skipping. " + transformedRecord.getJson().toJSONString());
      }
    }
    // Reached batch size, send off the batched records to FOLIO Inventory.
    if (batch.size() >= ctxt.batchSize) {

      boolean batchSucceeded = releaseBatch();

      if (! batchSucceeded) {
        logger.info("Falling back to single record-set upserts after encountering one or more errors in the attempted batch upsert.");
        // To get all successful records upserted, and obtain error messages for all problems.
        for (TransformedRecord record : batch) {
          try {
            addSingleRecord(record.getRecordJSON());
          } catch (NullPointerException npe) {
            logger.info("Internal error Inventory Updater: " + npe.getMessage());
            npe.printStackTrace();
          } catch (IndexOutOfBoundsException ioobe) {
            logger.info("Internal error Inventory Updater: " + ioobe.getMessage());
            ioobe.printStackTrace();
          }
        }
        batch = new ArrayList<>();
      }

    }
  }

  public boolean gotAllHrids(TransformedRecord transformedRecord) {
    if (transformedRecord.getInstance().get("hrid") == null) {
      return false;
    } else {
      if (transformedRecord.getHoldings().isEmpty()) {
        return true;
      } else {
        for (Object h : transformedRecord.getHoldings()) {
          JSONObject holdingsJson = (JSONObject) h;
          if (holdingsJson.get("hrid") == null || ((String) holdingsJson.get("hrid")).isEmpty()) {
            return false;
          } else {
            for (Object i : (JSONArray) holdingsJson.get("items")) {
              JSONObject itemJson = (JSONObject) i;
              if (itemJson.get("hrid") == null || ((String) itemJson.get("hrid")).isEmpty()) {
                return false;
              }
            }
          }
        }
        return true;
      }
    }
  }

  public boolean releaseBatch() {
    JSONObject inventoryRecordSets = makeInventoryRecordSets(batch);
    try {
      long startStorageBatch = System.currentTimeMillis();
      CloseableHttpResponse response = batchUpsertInventoryRecordSets(inventoryRecordSets);
      int statusCode = response.getStatusLine().getStatusCode();
      String responseAsString;
      try {
        responseAsString = EntityUtils.toString(response.getEntity());
        response.close();
      } catch (IOException ioe) {
        logger.error("Could not retrieve response from FOLIO: " + ioe.getMessage());
        return false;
      }
      if (statusCode == 200) {
        ctxt.timingsStoringInventoryRecordSet.time(startStorageBatch, batch.size());
        ctxt.storageStatus.incrementAdd(batch.size());
        UpsertMetrics metrics = new UpsertMetrics((JSONObject) getResponseAsJson(responseAsString).get("metrics"));
        setCounters(metrics);
        logRecordCounts();
      } else {
        JSONObject problem = getResponseAsJson(responseAsString);
        if (problem.containsKey("errors")  && ((JSONArray) problem.get("errors")).size()>0) {
          JSONObject firstError = ( (JSONObject) ( (JSONArray) problem.get("errors") ).get(0) );
          if (firstError.containsKey("shortMessage")) {
            logger.error("Batch upsert encountered a problem: " + firstError.get("shortMessage"));
          } else if (firstError.containsKey("message")) {
            logger.error("Batch upsert encountered a problem: " + firstError.get("message"));
          } else {
            logger.error("Batch upsert encountered a problem: " + firstError.toJSONString());
          }
        } else {
          logger.error("Batch upsert encountered a problem: " + problem.toJSONString());
        }
        return false;
      }
    } catch (InventoryUpdateException iue) {
      logger.error("Error in batch upsert: " + iue.getMessage());
      return false;
    }
    for (TransformedRecord record : batch) {
      // update marc storage
    }
    batch = new ArrayList<>();
    return true;
  }

  private JSONObject makeInventoryRecordSets(List<TransformedRecord> transformedRecords) {
    JSONObject inventoryRecordSets = new JSONObject();
    JSONArray array = new JSONArray();
    inventoryRecordSets.put("inventoryRecordSets", array);
    for (TransformedRecord transformedRecord : transformedRecords) {
      array.add(makeInventoryRecordSet(transformedRecord));
    }
    return inventoryRecordSets;
  }

  private JSONObject makeInventoryRecordSet(TransformedRecord transformedRecord) {
    JSONObject inventoryRecordSet = new JSONObject();
    JSONObject instance = transformedRecord.getInstance();
    if ( transformedRecord.hasMatchKey() && ctxt.inventoryUpsertPath.contains( "matchkey" ) )
    {
      instance.put( "matchKey", transformedRecord.getMatchKey() );
    }
    inventoryRecordSet.put( "instance", instance );
    if ( transformedRecord.getHoldings() != null )
    {
      inventoryRecordSet.put( "holdingsRecords", transformedRecord.getHoldings() );
    }
    if ( transformedRecord.hasInstanceRelations() )
    {
      inventoryRecordSet.put( "instanceRelations", transformedRecord.getInstanceRelations() );
    }
    // Add processing info if provided by the transformation
    if ( transformedRecord.hasProcessingInfo() )
    {
      inventoryRecordSet.put( "processing", transformedRecord.getProcessingInfo() );
    }
    return inventoryRecordSet;
  }

  /**
   * Adds or updates a set of Inventory records (an Instance with related holdings and/or a source record)
   * from the incoming bibliographic record.
   * @param recordJSON The JSON coming into the Harvester Inventory storage logic from the transformation pipeline
   */
  public void addSingleRecord(RecordJSON recordJSON) {
    TransformedRecord transformedRecord = new TransformedRecord(recordJSON, logger);
    RecordWithErrors recordWithErrors = new RecordWithErrors(transformedRecord, failedRecordsController);
    try {
      long startStorageEntireRecord = System.currentTimeMillis();
      /*
       * TransformedRecord is a 'facade' that provides an API into RecordJSON and allows for (slight)
       * variations in the structure of the transformed record while shielding the client code from
       * those potential differences in structure. It's thus safer to use than RecordJSON.
       */

      if (transformedRecord.isRecordExcludedByDateFilter(ctxt)) {
          updateCounters.xmlBulkRecordsSkipped++;
          ctxt.timingsCreatingRecord.setTiming( recordJSON.getCreationTiming() );
          ctxt.timingsTransformingRecord.setTiming( recordJSON.getTransformationTiming() );
      } else {
        JSONObject inventoryRecordSet = makeInventoryRecordSet(transformedRecord);
        logger.log( Level.TRACE, "Sending upsert request with : " + inventoryRecordSet.toJSONString() );
        JSONObject responseJson = upsertInventoryRecordSet( inventoryRecordSet, recordWithErrors );
        logger.log( Level.TRACE, "Response was: " + responseJson.toJSONString() );
        UpsertMetrics metrics = new UpsertMetrics( (JSONObject) responseJson.get( "metrics" ) );

        if ( ctxt.harvestable.isStoreOriginal() )
        {
          if ( metrics.instance.create.failed == 0 )
          {
            String institutionId = transformedRecord.getInstitutionId( locInstMap );
            String localIdentifier = transformedRecord.getLocalIdentifier();

            updateCounters.sourceRecordsProcessed++;
            if ( ctxt.marcStorageUrlIsDefined )
            {
              JSONObject marcJson = getMarcJson( recordWithErrors);
              JSONObject instanceJson = (JSONObject) responseJson.get( "instance" );
              String instanceId = instanceJson.get( "id" ).toString();
              addOrUpdateMarcRecord( marcJson, instanceId, institutionId, localIdentifier, recordWithErrors );
            }
            else
            {
              updateCounters.sourceRecordsFailed++;
              RecordError error = new ExceptionRecordError( new InventoryUpdateException(
                      "Configuration error: Cannot store original content as requested, no path configured for MARC storage" ),
                      "Missing configuration: [" + InventoryUpdateContext.MARC_STORAGE_PATH + "]",
                      InventoryUpdateContext.FAILURE_ENTITY_TYPE_SOURCE_RECORD, "", "" );
              recordWithErrors.reportError( error, Level.DEBUG );
            }
          }
          else
          {
            RecordError error = new ExceptionRecordError( new InventoryUpdateException(
                    "Instance error: Cannot store original content as requested since Instance creation failed" ),
                    "Missing Instance, source record skipped: [" + InventoryUpdateContext.MARC_STORAGE_PATH + "]",
                    InventoryUpdateContext.FAILURE_ENTITY_TYPE_SOURCE_RECORD, "", "" );
            recordWithErrors.reportError( error, Level.DEBUG );

          }
        }
        ctxt.timingsStoringInventoryRecordSet.time( startStorageEntireRecord );
        ctxt.timingsCreatingRecord.setTiming( recordJSON.getCreationTiming() );
        ctxt.timingsTransformingRecord.setTiming( recordJSON.getTransformationTiming() );
        ctxt.storageStatus.incrementAdd( 1 );
        setCounters( metrics );
        logRecordCounts();
        if ( recordWithErrors.hasErrors() )
        {
          recordWithErrors.logFailedRecord();
        }
      }
    } catch (InventoryUpdateException iue) {
        recordWithErrors.writeErrorsLog(logger);
        recordWithErrors.logFailedRecord();
    }
  }

  private void logRecordCounts() {
    //if (updateCounters.instancesLoaded>0 && updateCounters.instancesLoaded % (updateCounters.instancesLoaded < 1000 ? 100 : 1000) == 0) {
    if ((updateCounters.lastLogOfInstancesProcessed < 1000 && updateCounters.instancesProcessed >= updateCounters.lastLogOfInstancesProcessed + 100)
        || (updateCounters.instancesProcessed >= updateCounters.lastLogOfInstancesProcessed + 1000)) {
      logger.info("" + updateCounters.instancesLoaded + " instances, "
              + updateCounters.holdingsRecordsLoaded + " holdings records, "
              + updateCounters.itemsLoaded + " items, and "
              + updateCounters.sourceRecordsLoaded + " source records ingested. "
              + updateCounters.instanceDeleteSignals + " delete signal(s), "
              + updateCounters.instanceDeletions + " delete(s)"
              + (updateCounters.xmlBulkRecordsSkipped >0 ? " -- " + updateCounters.xmlBulkRecordsSkipped + " record(s) skipped by filter" : ""));
      updateCounters.lastLogOfInstancesProcessed = updateCounters.instancesProcessed;

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
      holdingsRecord = new EntityMetrics((JSONObject) json.get("HOLDINGS_RECORD"));
      item = new EntityMetrics((JSONObject) json.get("ITEM"));
    }

  }

  public static class EntityMetrics {

    TransactionMetrics create;
    TransactionMetrics update;
    TransactionMetrics delete;
    long processed;
    long failed;
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

  public static class TransactionMetrics {

    Long completed = 0L;
    Long failed = 0L;
    Long skipped = 0L;
    Long processed = 0L;

    public TransactionMetrics (JSONObject transactionMetricsJson) {

      if (transactionMetricsJson != null) {
        completed =  (Long) transactionMetricsJson.get("COMPLETED");
        failed = (Long) transactionMetricsJson.get("FAILED");
        skipped = (Long) transactionMetricsJson.get("SKIPPED");
        processed = completed+failed+skipped;
      }
    }

  }

  private JSONObject upsertInventoryRecordSet(JSONObject inventoryRecordSet, RecordWithErrors recordWithErrors) throws InventoryUpdateException {
    JSONObject upsertResponse;
    try {
      String url = ctxt.folioAddress + ctxt.inventoryUpsertPath;
      HttpEntityEnclosingRequestBase httpUpdate = new HttpPut(url);
      StringEntity entity = new StringEntity(inventoryRecordSet.toJSONString(), "UTF-8");
      httpUpdate.setEntity(entity);
      setHeaders(httpUpdate,"application/json");
      CloseableHttpResponse response = ctxt.folioClient.execute(httpUpdate);
      String responseAsString = EntityUtils.toString(response.getEntity());
      response.close();
      upsertResponse = getResponseAsJson(responseAsString);
      checkForInventoryServiceErrors(response, responseAsString, recordWithErrors);
      checkForRecordErrors(response, upsertResponse, recordWithErrors);
    } catch (StorageException se) {
      throw se;
    } catch (Exception e) {
      throw new InventoryUpdateException("Inventory Upsert encountered an exception", e);
    }
    return upsertResponse;
  }

  private CloseableHttpResponse batchUpsertInventoryRecordSets (JSONObject inventoryRecordSets) throws InventoryUpdateException {
    try {
      String url = ctxt.folioAddress + ctxt.inventoryBatchUpsertPath;
      HttpEntityEnclosingRequestBase httpUpdate = new HttpPut(url);
      StringEntity entity = new StringEntity(inventoryRecordSets.toJSONString(), "UTF-8");
      httpUpdate.setEntity(entity);
      setHeaders(httpUpdate,"application/json");
      return ctxt.folioClient.execute(httpUpdate);
    } catch (StorageException se) {
      throw se;
    } catch (Exception e) {
      throw new InventoryUpdateException("Inventory Upsert encountered an exception", e);
    }
  }

  private void checkForRecordErrors(CloseableHttpResponse response, JSONObject upsertResponse, RecordWithErrors recordWithErrors) {
    if (upsertResponse.containsKey("errors")) {
      JSONArray errorsArray = (JSONArray) upsertResponse.get("errors");
      for (int i=0; i< errorsArray.size(); i++)  {
        JSONObject errorJson = (JSONObject) errorsArray.get(i);
        String countingMessage;
        if (errorJson.get("shortMessage") != null) {
          countingMessage = (String) errorJson.get("shortMessage");

        } else {
          if (errorJson.get("message") instanceof JSONObject) {
            countingMessage = Jsoner.prettyPrint(( (JSONObject) errorJson.get("message") ).toJSONString());
          } else {
            countingMessage = errorJson.get("message").toString();
          }
        }
        RecordError error = new HttpRecordError(
                response.getStatusLine().getStatusCode(),
                response.getStatusLine().getReasonPhrase(),
                errorJson.containsKey("message") ? errorJson.get("message").toString() : "",
                countingMessage,
                "Error encountered during upsert of Inventory record set",
                errorJson.containsKey("entityType") ? errorJson.get("entityType").toString() : "",
                errorJson.containsKey("transaction") ? errorJson.get("transaction").toString() : "",
                errorJson.containsKey("entity") ? errorJson.get("entity").toString() : "");
        if (i==0) {
          recordWithErrors.reportError(error, Level.DEBUG);
        } else {
          recordWithErrors.addError(error);
        }
      }
    }
  }

  private void checkForInventoryServiceErrors(CloseableHttpResponse response, String responseAsString, RecordWithErrors recordWithErrors) {
      final int statusCode = response.getStatusLine().getStatusCode();
      if (!Arrays.asList(200,201,422).contains(statusCode)) {
        updateCounters.instancesFailed++;
        final RecordError error = new HttpRecordError(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase(), responseAsString, responseAsString, "Error upserting Inventory record set", "InventoryRecordSet", "upsert", "{}");
        recordWithErrors.reportError(error, Level.DEBUG);
        logger.debug(String.format("Error %s, %s upserting Inventory record set", response.getStatusLine().getStatusCode(), error.getShortMessageForCounting()));
        if (Arrays.asList(403,500).contains(statusCode)) {
          throw new StorageException(error.getMessageWithContext());
        } else if (statusCode == 404 && responseAsString.contains("No suitable module found for path")) {
          throw new StorageException(error.getMessageWithContext());
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
   * Create JSONObject from the XML in the incoming record
   * @return
   * @throws InventoryUpdateException
   */
  private JSONObject getMarcJson(RecordWithErrors recordWithErrors) throws InventoryUpdateException {
    JSONObject marcJson = null;
    TransformedRecord record = recordWithErrors.transformedRecord;
    if (record.getOriginalContent() != null) {
      try {
        String originalContentString = new String(record.getOriginalContent(), StandardCharsets.UTF_8 );
        if(originalContentString.startsWith("<") ) {
          logger.log(Level.TRACE,"Treating source record as XML");
          marcJson = MarcXMLToJson.convertMarcXMLToJson(originalContentString);
        } else {
          logger.log(Level.TRACE,"Treating source record as ISO2079");
          marcJson = MarcToJson.convertMarcRecordsToJson(record.getOriginalContent()).get(0);
        }
        logger.log(Level.TRACE,marcJson.toJSONString());
      } catch (IOException | ParserConfigurationException | SAXException e) {

        updateCounters.sourceRecordsFailed++;
        RecordError error = new ExceptionRecordError(e, "Error creating MARC JSON for source record", "MARC source", "GET", "");
        recordWithErrors.reportAndThrowError(error, Level.DEBUG);
      }
    } else {
      if (ctxt.harvestable.isStoreOriginal()) {
        updateCounters.sourceRecordsFailed++;
        RecordError error = new ExceptionRecordError(new InventoryUpdateException("Job set to store original source but no original content found."),
                                              "Job set to store original source but no original content found.",
                                              "MARC source", "preparation", "");
        recordWithErrors.reportAndThrowError(error, Level.DEBUG);
      }
    }
    return marcJson;
  }

  /**
   * Looks for existing MARC record in FOLIO by uniquely identifying criteria
   * @param instanceId
   * @param institutionId
   * @param localIdentifier
   * @return
   * @throws InventoryUpdateException
   */
  private JSONObject getExistingMarcRecord(String instanceId, String institutionId, String localIdentifier, RecordWithErrors recordWithErrors) throws InventoryUpdateException {
    long startGetExistingMarc = System.currentTimeMillis();
    JSONObject marcRecord = null;
    try {
      StringBuilder query = new StringBuilder().append("(instanceId==\"").append(instanceId).append("\"").append(" and institutionId==\"").append(institutionId).append("\"").append(" and localIdentifier==\"").append(localIdentifier).append("\")");
      StringBuilder url;
      url = new StringBuilder().append(ctxt.marcStorageUrl).append("?query=").append(URLEncoder.encode(query.toString(), "UTF-8"));
      HttpGet httpGet = new HttpGet(url.toString());
      setHeaders(httpGet,"application/json");
      CloseableHttpResponse response = ctxt.folioClient.execute(httpGet);
      if (response.getStatusLine().getStatusCode() != 200) {
        RecordError error = new HttpRecordError(response.getStatusLine(), response.getEntity().toString(), response.getEntity().toString(), "Error looking up existing MARC record, expected status 200", "MARC source", "GET", "{}");
        recordWithErrors.reportAndThrowError(error, Level.DEBUG);
      } else {
        String responseAsString = EntityUtils.toString(response.getEntity());
        JSONObject marcRecords = (JSONObject) (new JSONParser().parse(responseAsString));
        final Long count = (Long) (marcRecords.getOrDefault("totalRecords", 0));
        if (count == 0) {
          logger.log(Level.TRACE,"No MARC source record found for instance [" + instanceId + "], institution [" + institutionId + "] and local identifier [" + localIdentifier + "]");
        } else if (count == 1) {
          logger.log(Level.TRACE, "Found existing MARC source record for instance [" + instanceId + "], institution [" + institutionId + "] and local identifier [" + localIdentifier + "]");
          JSONArray records = (JSONArray) marcRecords.get("marcrecords");
          marcRecord = (JSONObject) records.get(0);
        } else {
          logger.error("Expected zero or one MARC source records for instance [" + instanceId + "], institution [" + institutionId + "] and local identifier [" + localIdentifier + "] but count was " + count);
        }
      }
      response.close();
    } catch (IOException | ParseException e) {
      RecordError error = new ExceptionRecordError(e, "Error when checking for previously existing MARC record","MARC source","GET", "");
      recordWithErrors.reportAndThrowError(error, Level.DEBUG);
    }
    logger.log(Level.TRACE,"Got existing MARC in: " + (System.currentTimeMillis()-startGetExistingMarc));
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
  private JSONObject addOrUpdateMarcRecord(JSONObject marcJson, String instanceId, String institutionId, String localIdentifier, RecordWithErrors recordWithErrors) throws InventoryUpdateException {
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
        JSONObject marcRecord = getExistingMarcRecord(instanceId, institutionId, localIdentifier, recordWithErrors);
        if (marcRecord == null) {
          logger.log(Level.TRACE,"This MARC record did not exist in storage; creating it.");
          long startAddingMarc = System.currentTimeMillis();
          request = new HttpPost(ctxt.marcStorageUrl);
          request.setEntity(entity);
          setHeaders(request,"application/json");
          CloseableHttpResponse response = ctxt.folioClient.execute(request);
          String responseAsString = EntityUtils.toString(response.getEntity());
          marcResponse = getResponseAsJson(responseAsString);
          if (response.getStatusLine().getStatusCode() != 201) {
            updateCounters.sourceRecordsFailed++;
            RecordError error = new HttpRecordError(response.getStatusLine(), responseAsString, responseAsString, "Error adding MARC source record ", "MARC source", "POST", "{}");
            recordWithErrors.reportAndThrowError(error, Level.DEBUG);
          } else {
            logger.log(Level.TRACE, "Status code: " + response.getStatusLine().getStatusCode() + " for POST of marc json " + marcPostJson.toJSONString());
            updateCounters.sourceRecordsLoaded++;
          }
          response.close();
          logger.log(Level.TRACE, "Added MARC in " + (System.currentTimeMillis()-startAddingMarc));
        } else {
          logger.log(Level.TRACE, "This MARC record already existed in storage; updating it.");
          long startUpdatingMarc = System.currentTimeMillis();
          String id = (String) marcRecord.get("id");
          String url = ctxt.marcStorageUrl + "/" + id;
          request = new HttpPut(url);
          request.setEntity(entity);
          setHeaders(request,"text/plain");
          CloseableHttpResponse response = ctxt.folioClient.execute(request);
          response.close();
          if (response.getStatusLine().getStatusCode() != 204) {
            updateCounters.sourceRecordsFailed++;
            RecordError error = new HttpRecordError(response.getStatusLine(), response.getEntity().toString(), response.getEntity().toString(), "Error updating existing MARC record", "MARC source", "PUT", "{}");
            recordWithErrors.reportAndThrowError(error, Level.DEBUG);
          } else {
            logger.log(Level.TRACE,"Status code: " + response.getStatusLine().getStatusCode() + " for PUT of marc json " + marcPostJson.toJSONString());
            updateCounters.sourceRecordsLoaded++;
          }
          logger.log(Level.TRACE, "Updated MARC in " + (System.currentTimeMillis()-startUpdatingMarc));
        }
      } catch (IOException | org.apache.http.ParseException | UnsupportedCharsetException e) {
        updateCounters.sourceRecordsFailed++;
        RecordError error = new ExceptionRecordError(e, "Error adding MARC source record", "MARC source", "POST", "");
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
  private void deleteSourceRecord(String uuid, RecordWithErrors recordWithErrors) throws InventoryUpdateException {
    logger.log(Level.TRACE,"Deleting source record with ID: " + uuid);
    String url = String.format("%s/%s", ctxt.marcStorageUrl, uuid);
    HttpDelete httpDelete = new HttpDelete(url);
    setHeaders(httpDelete,"text/plain");
    CloseableHttpResponse response = null;
    try {
      response = ctxt.folioClient.execute(httpDelete);
      if (response.getStatusLine().getStatusCode() != 204) {
        RecordError error = new HttpRecordError(response.getStatusLine(), response.getEntity().toString(), response.getEntity().toString(), "Error deleting source record", "MARC source", "DELETE", "{}");
        recordWithErrors.reportAndThrowError(error, Level.DEBUG);
      } else {
        updateCounters.sourceRecordsDeleted++;
      }
    } catch (IOException e) {
      RecordError error = new ExceptionRecordError(e, "Error DELETEing MARC source record", "MARC source", "DELETE", "");
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
  public void deleteRecord(RecordJSON recordJSON) {
    TransformedRecord transformedRecord = new TransformedRecord(recordJSON, logger);
    RecordWithErrors recordWithErrors = new RecordWithErrors(transformedRecord, failedRecordsController);
    try {
      updateCounters.instanceDeleteSignals++;
      if (transformedRecord.isDeleted()) {
        logger.log(Level.TRACE, "Delete request received: " + transformedRecord.getDelete().toJSONString());
        JSONObject deletionJson = transformedRecord.getDelete();
        logger.info("Sending delete request " + transformedRecord.getJson().toJSONString() + " to " + ctxt.inventoryUpsertUrl);
        HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(ctxt.inventoryUpsertUrl);
        setHeaders(httpDelete,"application/json");
        StringEntity entity = new StringEntity(deletionJson.toJSONString(), "UTF-8");
        httpDelete.setEntity(entity);
        CloseableHttpResponse response = null;
        try {
          response = ctxt.folioClient.execute(httpDelete);
          String responseAsString = EntityUtils.toString(response.getEntity());
          JSONObject responseAsJson = getResponseAsJson(responseAsString);
          int statusCodeInstanceDelete = response.getStatusLine().getStatusCode();
          if (! Arrays.asList(200, 204, 404).contains(statusCodeInstanceDelete)) {
            logger.error("Error " + response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase());
            RecordError error = new HttpRecordError(response.getStatusLine(), responseAsString, responseAsString, "Error deleting source record", "MARC source", "DELETE", "{}");
            recordWithErrors.reportAndThrowError(error, Level.DEBUG);
          } else if (statusCodeInstanceDelete == 404) {
            logger.info(
                    "Did not find Instance to apply delete request to. Delete request body was: " + transformedRecord.getDelete().toJSONString());
            logger.debug("Delete request response: " + responseAsString);
          } else
          {
            if ( ctxt.marcStorageUrlIsDefined && false ) // disabling MARC source deletion due to indexing problem in mod-marc-storage
            {
              String oaiId = (String) deletionJson.get( "oaiIdentifier" );
              String localIdentifier = ( oaiId != null ? oaiId.substring( oaiId.lastIndexOf( ":" ) + 1 ) : null );
              if ( localIdentifier == null )
              {
                localIdentifier = (String) deletionJson.get( "localIdentifier" );
              }
              String institutionId = (String) deletionJson.get( "institutionId" );
              String instanceId;
              JSONObject instanceResponse = (JSONObject) responseAsJson.get( "instance" );
              if ( instanceResponse != null )
              {
                instanceId = (String) instanceResponse.get( "id" );
                deleteMarcSourceRecordForInstitution( localIdentifier, institutionId, instanceId, recordWithErrors );
              }
              else
              {
                logger.error(
                        "Could not obtain ID of the Instance that a delete request was sent for. Consequently also no MARC record was deleted." );
              }
            }
          }
          UpsertMetrics metrics = new UpsertMetrics((JSONObject)responseAsJson.get("metrics"));
          logger.debug("metrics: " + responseAsJson.toJSONString());
          setCounters(metrics);
          logRecordCounts();
        } catch (IOException e) {
          RecordError error = new ExceptionRecordError(e, "Error DELETEing Inventory record set", "InventoryRecordSet", "DELETE", "{}");
          recordWithErrors.reportAndThrowError(error, Level.DEBUG, e);
        } finally {
          if (response != null) {
            try {
              response.close();
            } catch (IOException e) {
              // TODO: throw inside finally
              throw new StorageException("Couldn't close response after DELETE Inventory record set request", e);
            }
          }
        }

      }
      if (recordWithErrors.hasErrors()) {
        recordWithErrors.writeErrorsLog(logger);
      }
    } catch (InventoryUpdateException iue) {
      logger.error(iue.getMessage());
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
  private void deleteMarcSourceRecordForInstitution(String localIdentifier, String institutionId, String instanceId, RecordWithErrors recordWithErrors) throws InventoryUpdateException {
    JSONObject marcRecord = getExistingMarcRecord(instanceId, institutionId, localIdentifier, recordWithErrors);
    if (marcRecord != null) {
      String sourceId = (String) marcRecord.get("id");
      deleteSourceRecord(sourceId, recordWithErrors);
    } else {
      logger.log(Level.DEBUG,"Found no source record to delete for instance [" + instanceId + "], institution [" + institutionId + "] and local identifier [" + localIdentifier + "]");
    }
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

}
