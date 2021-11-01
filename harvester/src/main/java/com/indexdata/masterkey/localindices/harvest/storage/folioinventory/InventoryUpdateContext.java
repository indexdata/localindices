package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.StorageException;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Level;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class InventoryUpdateContext {

    public Harvestable harvestable;
    protected static final String FOLIO_TENANT = "folioTenant";
    protected static final String FOLIO_AUTH_PATH = "folioAuthPath";
    protected static final String FOLIO_USERNAME = "folioUsername";
    protected static final String FOLIO_PASSWORD = "folioPassword";
    protected static final String FOLIO_AUTH_SKIP = "folioAuthSkip";
    protected static final String MARC_STORAGE_PATH = "marcStoragePath";
    protected static final String INVENTORY_UPSERT_PATH = "inventoryUpsertPath";

    public String folioAddress;
    private JSONObject storageConfig;
    public String folioTenant;
    public String folioAuthPath;
    public String folioUsername;
    public String folioPassword;
    public boolean folioAuthSkip = false;
    public String marcStoragePath;
    public String inventoryUpsertPath;

    public String marcStorageUrl;
    public String inventoryUpsertUrl;
    public boolean marcStorageUrlIsDefined;

    public String authToken;

    public StorageJobLogger logger;
    public final Map<String,String> locationsToInstitutionsMap = new HashMap<>();
    public FailedRecordsController failedRecordsController;
    public RecordUpdateCounters updateCounters;
    public HourlyPerformanceStats timingsStoringInventoryRecordSet;
    public HourlyPerformanceStats timingsCreatingRecord;
    public HourlyPerformanceStats timingsTransformingRecord;
    public InventoryStorageStatus storageStatus;
    private final boolean isXmlBulkJob;
    private Instant xmlBulkRecordFilteringDate;

    public CloseableHttpClient inventoryClient = null;

    public static final String FAILURE_ENTITY_TYPE_SOURCE_RECORD = "source record";

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
        isXmlBulkJob = (harvestable instanceof XmlBulkResource);
        if (isXmlBulkJob && !harvestable.getOverwrite()) {
            if (harvestable.getFromDate() != null) {
                xmlBulkRecordFilteringDate = harvestable.getFromDate().toInstant();
            } else if (harvestable.getLastHarvestFinished() != null) {
                xmlBulkRecordFilteringDate = harvestable.getLastHarvestFinished().toInstant();
            }
            if (xmlBulkRecordFilteringDate != null) {
                logger.info(
                        "Filtering records by date, excluding records with an update date before " + xmlBulkRecordFilteringDate );
            }
        }
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
        folioTenant = getRequiredConfig(FOLIO_TENANT);
        folioAuthSkip = getConfig(FOLIO_AUTH_SKIP, "false").equalsIgnoreCase("true");
        if (!folioAuthSkip) {
            folioAuthPath = getRequiredConfig(FOLIO_AUTH_PATH);
            folioUsername = getRequiredConfig(FOLIO_USERNAME);
            folioPassword = getRequiredConfig(FOLIO_PASSWORD);
        }
        inventoryUpsertPath = getConfig(INVENTORY_UPSERT_PATH);
        inventoryUpsertUrl = (inventoryUpsertPath != null ? folioAddress + inventoryUpsertPath : null);
        marcStoragePath = getConfig(MARC_STORAGE_PATH);
        marcStorageUrl = (marcStoragePath != null ? folioAddress + marcStoragePath : null);
        marcStorageUrlIsDefined = marcStorageUrl != null;
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

    public String getConfig(String key, String defaultValue) {
        return (storageConfig.get(key) != null ? (String) storageConfig.get(key) : "false" );
    }

    /**
     * If harvestable is an XML bulk job and 'overwrite' is not checked, and if the job has a 'fromDate' or
     * alternatively a 'lastHarvestFinished' date, then date filtering applies
     * @return true if date filtering applies for this job.
     */
    public boolean xmlBulkRecordFilteringApplies() {
        return xmlBulkRecordFilteringDate != null;
    }

    public Instant getBulkRecordFilteringInstant() {
        return xmlBulkRecordFilteringDate;
    }

}