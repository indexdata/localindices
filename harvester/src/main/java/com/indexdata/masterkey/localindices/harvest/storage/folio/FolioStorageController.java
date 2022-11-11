package com.indexdata.masterkey.localindices.harvest.storage.folio;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import com.indexdata.masterkey.localindices.entity.Storage;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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
  private FolioUpdateContext context;

  private FolioRecordUpdater recordStorageHandler;

  @Override
  public void setHarvestable(Harvestable harvestable) {
    this.harvestable = harvestable;
    init();
  }

  /**
   * Initializes storage logger.
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
    if (!storageConfigJson.isEmpty()) {
      if (storageConfigJson.containsKey("inventoryUpsertPath")) {
        logger.info("Attaching job to Inventory Storage");
        context = new InventoryUpdateContext(harvestable, logger);
        recordStorageHandler = new InventoryRecordUpdater((InventoryUpdateContext) context);
      } else if (storageConfigJson.containsKey("sharedIndexPath")) {
        logger.info("Attaching job to Shared Index Storage");
        context = new ShareIndexUpdateContext(harvestable, logger);
        recordStorageHandler = new SharedIndexUpdater((ShareIndexUpdateContext) context);
      } else {
        throw new StorageException("No valid FOLIO inventory config found. Config must contain "
                + "either an 'inventoryUpsertPath' or a 'sharedIndexPath'. Abandoning job.");
      }
    } else {
      throw new StorageException("No FOLIO storage config found. Abandoning job.");
    }
    logger.info("Starting job [" + database + "]");
    logger.info("Main storage URL [" + context.folioAddress + context.getStoragePath());
    if (context.folioAuthSkip) logger.info("Storage configured to skip FOLIO authentication!");

    client = HttpClients.createDefault();
    context.setClient(client);
    if (!context.folioAuthSkip) {
      authenticateToFolio();
    }
    context.moduleDatabaseStart(database, properties);
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
                                    context.folioAddress,
                                    context.folioAuthPath,
                                    context.folioUsername,
                                    context.folioPassword,
                                    context.folioTenant);
    context.setAuthToken(authToken);
    logger.info("Authenticated to FOLIO Inventory, tenant [" + context.folioTenant + "]");
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
          recordStorageHandler.addRecord((RecordJSON) subRecord);
        }
      }
    } else {
      logger.log(Level.TRACE, "FOLIO Storage received add signal with a single record payload.");
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
    recordStorageHandler.deleteRecord((RecordJSON) record);
  }

  /**
   * Logs statistics at end of job
   */
  @Override
  public void databaseEnd() {
    logger.debug("Folio RecordStorage: databaseEnd() invoked.");
    recordStorageHandler.releaseBatch();
    context.moduleDatabaseEnd();
  }

  @Override
  public StorageStatus getStatus() throws StatusNotImplemented {
    return this.context.storageStatus;
  }


  // Unsupported interface methods
  @Override
  public void begin() throws IOException {
    logger.debug("Transaction begin request received by FOLIO RecordStorage (noop)");
  }

  @Override
  public void commit() throws IOException {
  }

  @Override
  public void rollback() throws IOException {
    logger.debug("Rollback request received by FOLIO RecordStorage (noop)");
  }

  @Override
  public void shutdown() throws IOException {
    logger.info("Shutdown request received by FOLIO RecordStorage - writing status, saving logs");
    databaseEnd();
    persistLogsInFolio();
  }

  private void persistLogsInFolio() {
    try {
      if (((InventoryUpdateContext)context).logHistoryStorageUrlIsDefined) {
        String url = ((InventoryUpdateContext)context)
            .logHistoryStorageUrl.replace("{id}", harvestable.getId().toString());
        HttpGet httpGet = new HttpGet(url);
        context.setHeaders(httpGet,"application/json");
        CloseableHttpResponse response = context.folioClient.execute(httpGet);
        if (response.getStatusLine().getStatusCode() == 200) {
          logger.info("Logs persisted in FOLIO Harvester Admin");
        } else {
          logger.error("Request to persist logs at " + url + " returned ["
              + response.getStatusLine().getStatusCode() + "]: "
              + response.getStatusLine().getReasonPhrase());
        }
      }
    } catch (Exception e) {
      logger.error("Error persisting harvest log: " + e.getMessage());
    }

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
    logger.info("Batch size: " + limit);
  }

}
