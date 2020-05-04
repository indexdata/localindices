package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.util.HashMap;
import java.util.Map;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.StorageException;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Level;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class InventoryUpdateContext {

    public Harvestable harvestable;
    protected static final String FOLIO_AUTH_PATH = "folioAuthPath";
    protected static final String FOLIO_TENANT = "folioTenant";
    protected static final String FOLIO_USERNAME = "folioUsername";
    protected static final String FOLIO_PASSWORD = "folioPassword";
    protected static final String INSTANCE_STORAGE_PATH = "instanceStoragePath";
    protected static final String HOLDINGS_STORAGE_PATH = "holdingsStoragePath";
    protected static final String ITEM_STORAGE_PATH = "itemStoragePath";

    public String folioAddress;
    private JSONObject storageConfig;
    public String folioAuthPath;
    public String folioTenant;
    public String folioUsername;
    public String folioPassword;
    public String instanceStoragePath;
    public String holdingsStoragePath;
    public String itemStoragePath;

    public String instanceStorageUrl;
    public String holdingsStorageUrl;
    public String itemStorageUrl;

    public String authToken;

    public StorageJobLogger logger;
    public final Map<String,String> locationsToInstitutionsMap = new HashMap<String,String>();
    public FailedRecordsController failedRecordsController;
    public RecordUpdateCounters updateCounters;
    public HourlyPerformanceStats timingsStoringInventoryRecordSet;
    public HourlyPerformanceStats timingsCreatingRecord;
    public HourlyPerformanceStats timingsTransformingRecord;
    public InventoryStorageStatus storageStatus;

    public CloseableHttpClient inventoryClient = null;

    /**
     * Holds job-wide configurations, settings, and objects - for example a shared Inventory HTTP client,
     * storage configuration parameters, the job logger, various counters for reporting job progress and
     * the controller for gathering and reporting errors encountered during the job.
     *
     * @param harvestable the job definition from which to obtain configuration parameters for the job
     * @param logger the job logger
     * @throws StorageException if mandatory configuration parameters are not found
     */
    public InventoryUpdateContext (Harvestable harvestable, StorageJobLogger logger) throws StorageException {
        this.logger = logger;
        this.harvestable = harvestable;
        Storage storage = harvestable.getStorage();
        setStorageConfig(storage);
        storageStatus = new InventoryStorageStatus();
        updateCounters = new RecordUpdateCounters();
        timingsStoringInventoryRecordSet = new HourlyPerformanceStats("Storing Inventory records", logger);
        timingsCreatingRecord = new HourlyPerformanceStats("Creating DOM for incoming record", logger);
        timingsCreatingRecord.setLogLevelForIntervals(Level.DEBUG);
        timingsTransformingRecord = new HourlyPerformanceStats("Transforming incoming record before storing", logger);
        timingsTransformingRecord.setLogLevelForIntervals(Level.DEBUG);
        failedRecordsController = new FailedRecordsController(logger, harvestable);
    }

    public void setClient (CloseableHttpClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    public void setAuthToken (String token) {
        this.authToken = token;
    }

    public void setStorageStatus(InventoryStorageStatus status) {
        this.storageStatus = status;
    }

    public void setLocationsToInstitutionsMap(Map<String,String> locationsToInstitutions) {
        this.locationsToInstitutionsMap.putAll(locationsToInstitutions);
    }

    private void setStorageConfig(Storage storage) {
        folioAddress = storage.getUrl();
        String configurationsJsonString = storage.getJson();
        if (configurationsJsonString != null && configurationsJsonString.length()>0) {
            try {
                JSONParser parser = new JSONParser();
                storageConfig = (JSONObject) parser.parse(configurationsJsonString);
            } catch (ParseException pe) {
                String error = "Could not parse JSON configuration from harvestable.json [" + configurationsJsonString + "]";
                logger.error(error + pe.getMessage());
                throw new StorageException (error,pe);
            }
        } else {
            String error = "Cannot find required configuration for Inventory storage (looking in STORAGE.JSON). Cannot perform job.";
            logger.error(error);
            throw new StorageException(error);
        }
        setStorageConfigs();

    }

    private void setStorageConfigs() {
        folioAuthPath = getRequiredConfig(FOLIO_AUTH_PATH);
        folioTenant = getRequiredConfig(FOLIO_TENANT);
        folioUsername = getRequiredConfig(FOLIO_USERNAME);
        folioPassword = getRequiredConfig(FOLIO_PASSWORD);

        instanceStoragePath = getRequiredConfig(INSTANCE_STORAGE_PATH);
        instanceStorageUrl = folioAddress + instanceStoragePath;

        holdingsStoragePath = getRequiredConfig(HOLDINGS_STORAGE_PATH);
        holdingsStorageUrl = folioAddress + holdingsStoragePath;

        itemStoragePath = getRequiredConfig(ITEM_STORAGE_PATH);
        itemStorageUrl = folioAddress + itemStoragePath;
    }

    private String getRequiredConfig(String key) throws StorageException {
        String val = (String) storageConfig.get(key);
        if (val == null || val.length()==0) {
            logger.error("Missing required Inventory storage configuration for ["+ key + "].");
            throw new StorageException("Missing mandatory configuration value [" + key + "]. Cannot perform harvest job");
        } else {
            return val;
        }
    }

    public String getConfig(String key) {
        return (String) storageConfig.get(key);
    }

}