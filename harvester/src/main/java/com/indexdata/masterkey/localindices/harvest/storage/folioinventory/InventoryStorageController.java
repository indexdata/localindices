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
 * Contains the logic for initializing storage, loggers, and update statistics.
 *
 * Iterates Inventory record sets (instances, holdings, items, source records) coming
 * in from the transformation pipeline and forwards each incoming record to
 * {@link InventoryRecordUpdater} for creation/update/deletion in FOLIO Inventory.
 *
 * @author kurt
 */
public class InventoryStorageController implements RecordStorage {
  protected CloseableHttpClient client;
  protected StorageJobLogger logger;
  protected Harvestable harvestable;

  protected Map<String, String> databaseProperties;
  private boolean statusWritten = false;
  private InventoryUpdateContext ctxt;

  @Override
  public void setHarvestable(Harvestable harvestable) {
    this.harvestable = harvestable;
    init();
  }

  /**
   * Initializes storage job with regards to storage URL, Inventory API paths,
   * logger, failed records logging, and various update statistics.
   *
   * All job-wide settings and objects, that are supposed to be "static" for
   * the duration of the Harvest job, are stored in InventoryUpdateContext which
   * is passed as 'context' to the executing classes.
   *
   */
  private void init() {
    try {
      logger = new FileStorageJobLogger(InventoryStorageController.class, harvestable);
    } catch (StorageException e) {
      throw new RuntimeException("Unable to init: " + e.getLocalizedMessage(), e);
    }
  }

  @Override
  public void databaseStart(String database, Map<String, String> properties) throws StorageException {
    this.databaseProperties = properties;
    ctxt = new InventoryUpdateContext(harvestable, logger);

    logger.info("Starting job [" + database + "]");
    logger.info("Storage URL [" + ctxt.folioAddress + (ctxt.useInventoryUpsert ? ctxt.inventoryUpsertPath : ctxt.instanceStoragePath));
    if (ctxt.folioAuthSkip) logger.info("Storage configured to skip FOLIO authentication!");

    client = HttpClients.createDefault();
    ctxt.setClient(client);
    if (!ctxt.folioAuthSkip) {
      authenticateToInventory();
    }

    ctxt.setLocationsToInstitutionsMap(getLocationsMap());
  }

  private void authenticateToInventory() throws StorageException {
    String authToken = getAuthtoken(client,
                                    ctxt.folioAddress,
                                    ctxt.folioAuthPath,
                                    ctxt.folioUsername,
                                    ctxt.folioPassword,
                                    ctxt.folioTenant);
    ctxt.setAuthToken(authToken);
    logger.info("Authenticated to FOLIO Inventory, tenant [" + ctxt.folioTenant + "]");
  }

  /**
   * Sends authentication POST request to FOLIO service
   * @param client
   * @param folioAddress
   * @param folioAuthPath
   * @param username
   * @param password
   * @param tenant
   * @return
   * @throws UnsupportedEncodingException
   * @throws IOException
   * @throws StorageException
   */
  @SuppressWarnings("unchecked")
  private String getAuthtoken(CloseableHttpClient client,
                              String folioAddress,
                              String folioAuthPath,
                              String username,
                              String password,
                              String tenant)
      throws  StorageException {
    try {
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
    } catch (IOException | org.apache.http.ParseException e) {
      throw new StorageException("Request to obtain FOLIO authtoken failed with " + e.getMessage());
    }
  }

  /**
   * Retrieve locations-to-institutions mappings from Inventory storage
   * Used for holdings/items deletion logic.
   * @throws IOException
   * @throws ParseException
   */
  private Map<String,String> getLocationsMap() throws StorageException {
    try {
      Map<String,String> locationsToInstitutions = new HashMap<String,String>();
      String url = String.format("%s", ctxt.folioAddress + "locations?limit=9999");
      HttpGet httpGet = new HttpGet(url);
      setHeaders(httpGet, "application/json");
      CloseableHttpResponse response = client.execute(httpGet);
      if(! Arrays.asList(200, 404).contains(response.getStatusLine().getStatusCode())) {
        throw new IOException(String.format("Got error '" +
                    response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase() + "' when retrieving locations",
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
          locationsToInstitutions.put((String)location.get("id"), (String)location.get("institutionId"));
        }
        logger.info("Initialized a map of " + locationsJson.size() + " FOLIO locations to institutions.");
        return locationsToInstitutions;
      } else {
        throw new StorageException("Failed to retrieve any locations from Inventory, found no 'locations' in response");
      }
    } catch (IOException | ParseException e) {
      throw new StorageException ("Error occurred trying to build map of locations to institutions from FOLIO Inventory: " + e.getMessage());
    }
  }

  /**
   * Invokes {@link InventoryRecordUpdater} to create or add records in Inventory from
   * an incoming {@link RecordJSON}
   */
  @Override
  public void add(Record recordJSON) {
    if (recordJSON.isCollection()) {
      logger.log(Level.TRACE, "Inventory Storage received add signal with a collection payload.");
      Collection<Record> subrecords = recordJSON.getSubRecords();
      if (subrecords.size()==1) {
        for (Record subRecord : subrecords) {
          logger.log(Level.TRACE, "Iterating subrecords of a RecordJSON of one subrecord");
          InventoryRecordUpdater recordStorageHandler = new InventoryRecordUpdater(ctxt);
          recordStorageHandler.addInventory((RecordJSON) subRecord);
        }
      } else {
        if (harvestable.isStoreOriginal()) {
          logger.warn("Store original content selected for this job, "
                  + "but storage layer received "
                  + "result with multiple metadata records and original content "
                  + "cannot be stored in that scenario.");
        }
        logger.log(Level.DEBUG, "Iterating multiple subrecords of RecordJSON");
        for (Record subRecord : subrecords) {
          InventoryRecordUpdater recordStorageHandler = new InventoryRecordUpdater(ctxt);
          recordStorageHandler.addInventory((RecordJSON) subRecord);
        }
      }
    } else {
      logger.log(Level.TRACE, "Inventory Storage received add signal with a single record payload.");
      InventoryRecordUpdater recordStorageHandler = new InventoryRecordUpdater(ctxt);
      recordStorageHandler.addInventory((RecordJSON) recordJSON);
    }
  }

  /**
   *  Invokes {@link InventoryRecordUpdater} to delete/modify Inventory records on
   *  an incoming delete request
   *
   */
  @Override
  public void delete(Record record) {
    InventoryRecordUpdater updater = new InventoryRecordUpdater(ctxt);
    updater.delete((RecordJSON) record);
  }

  /**
   * Logs statistics at end of job
   */
  @Override
  public void databaseEnd() {
    if (!statusWritten) {
      String instancesMessage = "Instances_processed/loaded/deletions(signals)/failed:__" + ctxt.updateCounters.instancesProcessed + "___" + ctxt.updateCounters.instancesLoaded + "___" + ctxt.updateCounters.instanceDeletions + "(" + ctxt.updateCounters.instanceDeleteSignals + ")___" + ctxt.updateCounters.instancesFailed + "_";
      String holdingsRecordsMessage = "Holdings_records_processed/loaded/deleted/failed:__" + ctxt.updateCounters.holdingsRecordsProcessed + "___" + ctxt.updateCounters.holdingsRecordsLoaded + "___" + ctxt.updateCounters.holdingsRecordsDeleted + "___" + ctxt.updateCounters.holdingsRecordsFailed + "_";
      String itemsMessage = "Items_processed/loaded/deleted/failed:__" + ctxt.updateCounters.itemsProcessed + "___" + ctxt.updateCounters.itemsLoaded + "___" + ctxt.updateCounters.itemsDeleted + "___" + ctxt.updateCounters.itemsFailed + "_";
      String sourceRecordsMessage = "Source_records_processed/loaded/deleted/failed:__" + ctxt.updateCounters.sourceRecordsProcessed + "___" + ctxt.updateCounters.sourceRecordsLoaded + "___" + ctxt.updateCounters.sourceRecordsDeleted + "___" + ctxt.updateCounters.sourceRecordsFailed + "_";

      logger.log((ctxt.updateCounters.instancesFailed>0 ? Level.WARN : Level.INFO), instancesMessage);
      logger.log((ctxt.updateCounters.holdingsRecordsFailed>0 ? Level.WARN : Level.INFO), holdingsRecordsMessage);
      logger.log((ctxt.updateCounters.itemsFailed>0 ? Level.WARN : Level.INFO), itemsMessage);
      logger.log((ctxt.updateCounters.sourceRecordsFailed>0 ? Level.WARN : Level.INFO), sourceRecordsMessage);

      ctxt.failedRecordsController.writeLog();
      ctxt.timingsCreatingRecord.writeLog();
      ctxt.timingsTransformingRecord.writeLog();
      ctxt.timingsStoringInventoryRecordSet.writeLog();
      harvestable.setMessage(instancesMessage + " " + holdingsRecordsMessage + " " + itemsMessage + " " + sourceRecordsMessage);
      statusWritten=true;
    }
  }

  @Override
  public StorageStatus getStatus() throws StatusNotImplemented {
    return this.ctxt.storageStatus;
  }

  private void setHeaders (HttpRequestBase request, String accept) {
    request.setHeader("Accept", accept);
    request.setHeader("Content-type", "application/json");
    request.setHeader("X-Okapi-Token", ctxt.authToken);
    request.setHeader("X-Okapi-Tenant", ctxt.folioTenant);
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
    logger.info("Inventory storage controller received shutdown request (not supported)");
  }

  @Override
  public void setBatchLimit(int limit) {
    throw new UnsupportedOperationException("set batch limit not supported.");
  }

}
