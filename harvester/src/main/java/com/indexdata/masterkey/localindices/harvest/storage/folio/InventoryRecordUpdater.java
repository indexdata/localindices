package com.indexdata.masterkey.localindices.harvest.storage.folio;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.github.cliftonlabs.json_simple.Jsoner;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.indexdata.masterkey.localindices.harvest.storage.RecordJSON;
import com.indexdata.masterkey.localindices.harvest.storage.StorageException;

import static com.indexdata.masterkey.localindices.harvest.storage.folio.TransformedRecord.BATCH_INDEX;
import static com.indexdata.masterkey.localindices.harvest.storage.folio.TransformedRecord.PROCESSING;

/**
 * Logic for handling create/update/delete for one or more FOLIO Inventory record
 * sets -- an Inventory record set being an instance with
 * holdings/items and possibly a source record, all created from one
 * incoming bibliographic record.
 * InventoryRecordUpdate gets context from {@link FolioStorageController},
 * for example the job logger and record update/failure counters for logging the
 * process.
 * @author ne
 */
 @SuppressWarnings("unchecked")
 public class InventoryRecordUpdater extends FolioRecordUpdater {

  protected InventoryUpdateContext ctxt;

  /** Overall updates and errors counters */
  private final InventoryRecordUpdateCounters updateCounters;
  private final FailedRecordsController failedRecordsController;
  private Map<Integer, RecordWithErrors> batch;
  private Integer batchIndex = 0;

  public InventoryRecordUpdater (InventoryUpdateContext ctxt) {
    this.ctxt = ctxt;
    logger = ctxt.logger;
    failedRecordsController = ctxt.failedRecordsController;
    updateCounters = ctxt.updateCounters;
    batch = new HashMap<>();
  }

  public static JSONObject getResponseAsJson(String responseAsString) {
    JSONObject upsertResponse;
    JSONParser parser = new JSONParser();
    try {
      upsertResponse = (JSONObject) parser.parse(responseAsString);
    } catch ( ParseException pe) {
      upsertResponse = new JSONObject();
      upsertResponse.put("wrappedErrorMessage", responseAsString);
    }
    return upsertResponse;
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
      RecordWithErrors recordWithErrors = new RecordWithErrors(transformedRecord, failedRecordsController);
      recordWithErrors.setBatchIndex(batchIndex++);
      batch.put(recordWithErrors.batchIndex, recordWithErrors);
      ctxt.timingsCreatingRecord.setTiming(recordJSON.getCreationTiming());
      ctxt.timingsTransformingRecord.setTiming(recordJSON.getTransformationTiming());
    }
    // Reached batch size, send off the batched records to FOLIO Inventory.
    if (batch.size() >= ctxt.batchSize) {
      releaseBatch();
    }
  }

  public void releaseBatch() {
    JSONObject inventoryRecordSets = makeInventoryRecordSets(batch.values());
    long startStorageBatch = System.currentTimeMillis();
    BatchUpsertResponse response;
    try {
      response = batchUpsertInventoryRecordSets(inventoryRecordSets);
    } catch (IOException ioe) {
      logger.error("There was a fatal error retrieving response from storage service: " + ioe.getMessage());
      return;
    }

    if (response.isError()) {
      logger.error("Batch update returned error code "
          + response.getErrorReport().getStatusCode() + ": " + response.getErrorReport().getMessage());
    } else {
      ctxt.timingsStoringInventoryRecordSet.time(startStorageBatch, batch.size());
      ctxt.storageStatus.incrementAdd(batch.size());
      setCounters(response.getMetrics());
      logRecordCounts();
      if (response.hasErrors()) {
        logger.error("Problem in upsert: " + response.getErrorsAsJsonArray().toJSONString());
        for (ErrorReport report : response.getErrors().values()) {
          RecordError error = new HttpRecordError(
                  report.getStatusCode(),
                  report.getMessage().toJSONString(),
                  report.getMessage().toJSONString(),
                  report.getShortMessage(),
                  "Error encountered during upsert of Inventory record set",
                  report.getEntityType(),
                  "",
                  report.getEntity().toJSONString(),
                  report.getRequestJson().toJSONString());
          if (report.getBatchIndex() != null) {
            batch.get(report.getBatchIndex()).reportError(error, Level.DEBUG);
            batch.get(report.getBatchIndex()).writeErrorsLog(logger);
            batch.get(report.getBatchIndex()).logFailedRecord();
          } else {
            logger.error("Could not get batch record by batchIndex, no index found: " + response.response.toJSONString());
          }
        }
      }
    }

    batch = new HashMap<>();
    batchIndex = 0;
  }

  private JSONObject makeInventoryRecordSets(Collection<RecordWithErrors> records) {
    JSONObject inventoryRecordSets = new JSONObject();
    JSONArray array = new JSONArray();
    inventoryRecordSets.put("inventoryRecordSets", array);
    for (RecordWithErrors recordWithErrors : records) {
      array.add(makeInventoryRecordSet(recordWithErrors));
    }
    return inventoryRecordSets;
  }

  private JSONObject makeInventoryRecordSet(RecordWithErrors record) {
    TransformedRecord transformedRecord = record.transformedRecord;
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
      inventoryRecordSet.put( PROCESSING, transformedRecord.getProcessingInfo() );
    }
    if (!inventoryRecordSet.containsKey(PROCESSING)) {
      inventoryRecordSet.put(PROCESSING, new JSONObject());
    }
    ((JSONObject) inventoryRecordSet.get(PROCESSING)).put(BATCH_INDEX, record.batchIndex);
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
      if (transformedRecord.isRecordExcludedByDateFilter(ctxt)) {
          updateCounters.xmlBulkRecordsSkipped++;
          ctxt.timingsCreatingRecord.setTiming( recordJSON.getCreationTiming() );
          ctxt.timingsTransformingRecord.setTiming( recordJSON.getTransformationTiming() );
      } else {
        JSONObject inventoryRecordSet = makeInventoryRecordSet(recordWithErrors);
        logger.log( Level.TRACE, "Sending upsert request with : " + inventoryRecordSet.toJSONString() );
        JSONObject responseJson = upsertInventoryRecordSet( inventoryRecordSet, recordWithErrors );
        logger.log( Level.TRACE, "Response was: " + responseJson.toJSONString() );
        UpsertMetrics metrics = new UpsertMetrics( (JSONObject) responseJson.get( "metrics" ) );

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
              + updateCounters.itemsLoaded + " items "
               + updateCounters.instancesDeleted + " delete(s)"
              + (updateCounters.xmlBulkRecordsSkipped >0 ? " -- " + updateCounters.xmlBulkRecordsSkipped + " record(s) skipped by filter" : ""));
      updateCounters.lastLogOfInstancesProcessed = updateCounters.instancesProcessed;

      if (updateCounters.instancesFailed + updateCounters.holdingsRecordsFailed + updateCounters.itemsFailed > 0) {
        logger.info("Failed: " + updateCounters.instancesFailed + " instances, " + updateCounters.holdingsRecordsFailed + " holdings records, and " + updateCounters.itemsFailed + " items");
      }
    }
  }

  private void setCounters (UpsertMetrics metrics) {
    updateCounters.holdingsRecordsDeleted += metrics.holdingsRecord.delete.completed;
    updateCounters.holdingsRecordDeletesSkipped += metrics.holdingsRecord.delete.skipped;
    updateCounters.holdingsRecordsFailed += (int) metrics.holdingsRecord.failed;
    updateCounters.holdingsRecordsLoaded += (int) (metrics.holdingsRecord.create.completed + metrics.holdingsRecord.update.completed);
    updateCounters.holdingsRecordsProcessed += (int) metrics.holdingsRecord.processed;
    updateCounters.instancesDeleted += metrics.instance.delete.completed;
    updateCounters.instanceDeletesSkipped += metrics.instance.delete.skipped;
    updateCounters.instancesFailed += (int) metrics.instance.failed;
    updateCounters.instancesLoaded += (int) (metrics.instance.create.completed + metrics.instance.update.completed);
    updateCounters.instancesProcessed += (int) metrics.instance.processed;
    updateCounters.itemsDeleted += metrics.item.delete.completed;
    updateCounters.itemDeletesSkipped += metrics.item.delete.skipped;
    updateCounters.itemsFailed += (int) metrics.item.failed;
    updateCounters.itemsLoaded += (int) (metrics.item.create.completed + metrics.item.update.completed);
    updateCounters.itemsProcessed += (int) metrics.item.processed;
  }

  public static class UpsertMetrics {

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
      processed = create.processed + update.processed + delete.processed;
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
      CloseableHttpResponse response = ctxt.executeWithToken(httpUpdate);
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

  private BatchUpsertResponse batchUpsertInventoryRecordSets (JSONObject inventoryRecordSets) throws IOException {
    String url = ctxt.folioAddress + ctxt.inventoryUpsertPath;
    HttpEntityEnclosingRequestBase httpUpdate = new HttpPut(url);
    StringEntity entity = new StringEntity(inventoryRecordSets.toJSONString(), "UTF-8");
    httpUpdate.setEntity(entity);
    setHeaders(httpUpdate,"application/json");
    CloseableHttpResponse response = ctxt.executeWithToken(httpUpdate);
    String responseAsString = EntityUtils.toString(response.getEntity());
    response.close();
    return new BatchUpsertResponse(response.getStatusLine().getStatusCode(), getResponseAsJson(responseAsString));
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
                errorJson.containsKey("entity") ? errorJson.get("entity").toString() : "",
                errorJson.containsKey("requestJson") ? errorJson.get("requestJson").toString() : "");
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
      if (!Arrays.asList(200,207).contains(statusCode)) {
        updateCounters.instancesFailed++;
        final RecordError error = new HttpRecordError(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase(), responseAsString, responseAsString, "Error upserting Inventory record set", "InventoryRecordSet", "upsert", "{}", "{}");
        recordWithErrors.reportError(error, Level.DEBUG);
        logger.debug(String.format("Error %s, %s upserting Inventory record set", response.getStatusLine().getStatusCode(), error.getShortMessageForCounting()));
        if (Arrays.asList(403,500).contains(statusCode)) {
          throw new StorageException(error.getMessageWithContext());
        } else if (statusCode == 404 && responseAsString.contains("No suitable module found for path")) {
          throw new StorageException(error.getMessageWithContext());
        }
      }
  }

  /**
   * Handles delete signal from the transformation pipeline
   * @param recordJSON contains deletion information
   */
  public void deleteRecord(RecordJSON recordJSON) {
    if (batchIndex > 0) {
      // Run pending upserts first to ensure the order of operations in the harvested file
      releaseBatch();
    }
    TransformedRecord transformedRecord = new TransformedRecord(recordJSON, logger);
    RecordWithErrors recordWithErrors = new RecordWithErrors(transformedRecord, failedRecordsController);
    try {
      if (transformedRecord.isDeleted()) {
        logger.log(Level.TRACE, "Delete request received: " + transformedRecord.getDelete().toJSONString());
        JSONObject deletionJson = transformedRecord.getDelete();
        logger.debug("Sending delete request " + transformedRecord.getJson().toJSONString() + " to " + ctxt.inventoryDeleteUrl);
        HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(ctxt.inventoryDeleteUrl);
        setHeaders(httpDelete,"application/json");
        StringEntity entity = new StringEntity(deletionJson.toJSONString(), "UTF-8");
        httpDelete.setEntity(entity);
        CloseableHttpResponse response = null;
        try {
          response = ctxt.executeWithToken(httpDelete);
          String responseAsString = EntityUtils.toString(response.getEntity());
          JSONObject responseAsJson = getResponseAsJson(responseAsString);
          int statusCodeInstanceDelete = response.getStatusLine().getStatusCode();
          if (! Arrays.asList(200, 204, 404).contains(statusCodeInstanceDelete)) {
            logger.error("Error " + response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase());
            RecordError error = new HttpRecordError(response.getStatusLine(), responseAsString, responseAsString, "Error deleting inventory record set", "Instance", "DELETE", "{}", "{}");
            recordWithErrors.reportAndThrowError(error, Level.DEBUG);
          } else if (statusCodeInstanceDelete == 404) {
            logger.debug("Delete request response: " + responseAsString);
          }
          UpsertMetrics metrics = new UpsertMetrics((JSONObject)responseAsJson.get("metrics"));
          logger.debug("metrics: " + responseAsJson.toJSONString());
          ctxt.storageStatus.incrementDelete(1);
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
              logger.error("Exception closing response in finally section: " + e.getMessage());
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
   * Convenience method setting the most common Inventory request headers
   */
  private void setHeaders (HttpRequestBase request, String accept) {
    request.setHeader("Accept", accept);
    request.setHeader("Content-type", "application/json");
    request.setHeader("X-Okapi-Tenant", ctxt.folioTenant);
  }

  private static class BatchUpsertResponse {
    int statusCode;
    public final String ERRORS = "errors";
    public final String METRICS = "metrics";
    JSONObject response;
    ErrorReport errorReport = new ErrorReport(null);

    public BatchUpsertResponse (int statusCode, JSONObject responseJson) {
      this.statusCode = statusCode;
      response = responseJson;
      if (isError()) {
        errorReport = new ErrorReport(responseJson);
      }
    }

    public boolean isError () {
      return !Arrays.asList(200,207).contains(statusCode);
    }

    public ErrorReport getErrorReport () {
      if (isError()) {
        return errorReport;
      } else {
        return new ErrorReport();
      }
    }

    public UpsertMetrics getMetrics () {
      if (response.containsKey(METRICS)) {
        return new UpsertMetrics((JSONObject) response.get(METRICS));
      } else {
        return new UpsertMetrics(null);
      }
    }

    public boolean hasErrors () {
      return ! getErrorsAsJsonArray().isEmpty();
    }

    public JSONArray getErrorsAsJsonArray () {
      return (JSONArray) response.getOrDefault(ERRORS, new JSONArray());
    }

    public Map<Integer, ErrorReport> getErrors () {
      Map<Integer,ErrorReport> reports = new HashMap<>();
      if (hasErrors()) {
        for (Object o : getErrorsAsJsonArray() ) {
          JSONObject err = (JSONObject) o;
          ErrorReport report = new ErrorReport(err);
          if (report.getBatchIndex()!=null) {
            reports.put(report.getBatchIndex(), report);
          }
        }
      }
      return reports;
    }

  }

  private static class ErrorReport {
    JSONObject json;
    public final String MESSAGE = "message";
    public final String SHORT_MESSAGE = "shortMessage";
    public final String ENTITY_TYPE = "entityType";
    public final String ENTITY = "entity";
    public final String STATUS_CODE = "statusCode";
    public final String REQUEST_JSON = "requestJson";

    public ErrorReport (JSONObject errorReportJson) {
      json = errorReportJson;
    }

    public ErrorReport () {
      json = new JSONObject();
    }

    public JSONObject getMessage () {
      if (json.get(MESSAGE) instanceof JSONObject) {
        return getJsonObject(MESSAGE);
      } else {
        JSONObject j = new JSONObject();
        j.put("message", getString(MESSAGE));
        return j;
      }
    }

    public int getStatusCode () {
      return getInt(STATUS_CODE);
    }

    public String getShortMessage () {
      return getString(SHORT_MESSAGE);
    }

    public String getEntityType () {
      return getString(ENTITY_TYPE);
    }

    public JSONObject getEntity () {
      return getJsonObject(ENTITY);
    }

    public JSONObject getRequestJson() {
      return json.containsKey(REQUEST_JSON) ? getJsonObject(REQUEST_JSON) : new JSONObject();
    }

    public Integer getBatchIndex () {
      return (getProcessingInstructions().containsKey(BATCH_INDEX) ?
              Integer.parseInt(getProcessingInstructions().get(BATCH_INDEX).toString()) :
              null);
    }

    public JSONObject getProcessingInstructions() {
      return getRequestJson() != null && getRequestJson().get(PROCESSING) != null ? (JSONObject) getRequestJson().get(PROCESSING) : new JSONObject();
    }

    private String getString(String key) {
      return (String) json.get(key);
    }

    private JSONObject getJsonObject(String key) {
      return (JSONObject) json.get(key);
    }

    private int getInt(String key) {
      if (json.containsKey(key)) {
        return Integer.parseInt(json.get(key).toString());
      } else {
        return -1;
      }
    }

  }
}

