package com.indexdata.masterkey.localindices.harvest.storage.folio;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Map;

import com.indexdata.masterkey.localindices.entity.Storage;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.json.simple.JSONObject;

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
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Implements RecordStorage in order to receive add-record-messages from the transformation
 * process.
 *
 * Contains the logic for initializing storage, loggers, and update statistics.
 *
 * Accepts Inventory record sets (instances, holdings, items, source records) coming
 * in from the transformation pipeline and forwards each incoming record to
 * {@link InventoryRecordUpdater} for creation/update/deletion in FOLIO Inventory.
 *
 * @author kurt
 */
public class FolioStorageController implements RecordStorage {
  protected CloseableHttpClient client;
  protected StorageJobLogger logger;
  protected Harvestable harvestable;

  protected Map<String, String> databaseProperties;
  private FolioUpdateContext ctxt;

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
      logger = new FileStorageJobLogger(FolioStorageController.class, harvestable);
    } catch (StorageException e) {
      throw new RuntimeException("Unable to init: " + e.getLocalizedMessage(), e);
    }
  }

  @Override
  public void databaseStart(String database, Map<String, String> properties) throws StorageException {
    this.databaseProperties = properties;
    JSONObject storageConfigJson = getStorageConfigJson(harvestable.getStorage());
    logger.debug("Storage config: " + storageConfigJson.toJSONString());
    if (storageConfigJson != null && !storageConfigJson.isEmpty()) {
      if (storageConfigJson.containsKey("inventoryUpsertPath")) {
        logger.info("Attaching job to Inventory Storage");
        ctxt = new InventoryUpdateContext(harvestable, logger);
      } else if (storageConfigJson.containsKey("sharedIndexPath")) {
        logger.info("Attaching job to Shared Index Storage");
        ctxt = new ShareIndexUpdateContext(harvestable, logger);
      } else {
        throw new StorageException("No valid FOLIO inventory config found. Config must contain "
                + "either an 'inventoryUpsertPath' or a 'sharedIndexPath'. Abandoning job.");
      }
    } else {
      throw new StorageException("No FOLIO storage config found. Abandoning job.");
    }
    logger.info("Starting job [" + database + "]");
    logger.info("Main storage URL [" + ctxt.folioAddress + ctxt.getStoragePath());
    if (ctxt.folioAuthSkip) logger.info("Storage configured to skip FOLIO authentication!");

    client = HttpClients.createDefault();
    ctxt.setClient(client);
    if (!ctxt.folioAuthSkip) {
      authenticateToFolio();
    }
    ctxt.moduleDatabaseStart(database, properties);
  }

  protected JSONObject getStorageConfigJson (Storage storage) {
    String configurationsJsonString = storage.getJson();
    if (configurationsJsonString != null && configurationsJsonString.length()>0) {
      try {
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(configurationsJsonString);
      } catch ( ParseException pe) {
        String error = "Could not parse JSON configuration from harvestable.json [" + configurationsJsonString + "]";
        logger.error(error + pe.getMessage());
        throw new StorageException (error,pe);
      }
    } else {
      String error = "Cannot find required configuration for Inventory storage (looking in STORAGE.JSON). Cannot perform job.";
      logger.error(error);
      throw new StorageException(error);
    }
  }


  private void authenticateToFolio() throws StorageException {
    String authToken = getAuthToken(client,
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
  private String getAuthToken(CloseableHttpClient client,
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

  FolioRecordUpdater getRecordUpdater () {
    if (ctxt instanceof InventoryUpdateContext) {
      return new InventoryRecordUpdater((InventoryUpdateContext) ctxt);
    }
    if (ctxt instanceof ShareIndexUpdateContext) {
      return new ShareIndexUpdater((ShareIndexUpdateContext) ctxt);
    }
    return null;
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
          FolioRecordUpdater recordStorageHandler = getRecordUpdater();
          recordStorageHandler.addRecord((RecordJSON) subRecord);
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
          FolioRecordUpdater recordStorageHandler = getRecordUpdater();
          recordStorageHandler.addRecord((RecordJSON) subRecord);
        }
      }
    } else {
      logger.log(Level.TRACE, "FOLIO Storage received add signal with a single record payload.");
      FolioRecordUpdater recordStorageHandler = getRecordUpdater();
      recordStorageHandler.addRecord((RecordJSON) recordJSON);
    }
  }

  /**
   *  Invokes {@link InventoryRecordUpdater} to delete/modify Inventory records on
   *  an incoming delete request
   *
   */
  @Override
  public void delete(Record record) {
    FolioRecordUpdater updater = getRecordUpdater();
    updater.deleteRecord((RecordJSON) record);
  }

  /**
   * Logs statistics at end of job
   */
  @Override
  public void databaseEnd() {
    logger.debug("Folio RecordStorage: databaseEnd() invoked.");
    ctxt.moduleDatabaseEnd();
  }

  @Override
  public StorageStatus getStatus() throws StatusNotImplemented {
    return this.ctxt.storageStatus;
  }


  // Unsupported interface methods
  @Override
  public void begin() throws IOException {
    logger.debug("Transaction begin request received by FOLIO RecordStorage (noop)");
  }

  @Override
  public void commit() throws IOException {
    logger.debug("Commit request received by FOLIO RecordStorage (noop)");
  }

  @Override
  public void rollback() throws IOException {
    logger.debug("Rollback request received by FOLIO RecordStorage (noop)");
  }

  @Override
  public void shutdown() throws IOException {
    logger.info("Shutdown request received by FOLIO RecordStorage - writing status");
    databaseEnd();
  }

  @Override
  public void purge(boolean commit) throws IOException {
    logger.debug("Purge request received, probably due to 'Overwrite' being checked ('Overwrite' disables date filtering for XML bulk, but 'purge' does nothing in Inventory context)");
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
    logger.error("Delete by ID [" + id + "] not supported for Inventory");
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
  public void setBatchLimit(int limit) {
    throw new UnsupportedOperationException("set batch limit not supported.");
  }

}
