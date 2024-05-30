package com.indexdata.masterkey.localindices.harvest.storage.folio;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.StorageException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class InventoryUpdateContext extends FolioUpdateContext {

    protected static final String LOG_HISTORY_STORAGE_PATH = "logHistoryStoragePath";
    protected static final String INVENTORY_UPSERT_PATH = "inventoryUpsertPath";
    protected static final String INVENTORY_BATCH_UPSERT_PATH = "inventoryBatchUpsertPath";

    public String inventoryUpsertPath;
    public String inventoryUpsertUrl;
    public String inventoryDeleteUrl;
    public String logHistoryStoragePath;
    public String logHistoryStorageUrl;
    public boolean logHistoryStorageUrlIsDefined;
    public Integer batchSize;
    public InventoryRecordUpdateCounters updateCounters;
    public HourlyPerformanceStats timingsStoringInventoryRecordSet;
    public HourlyPerformanceStats timingsCreatingRecord;
    public HourlyPerformanceStats timingsTransformingRecord;


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
        super(harvestable, logger);
        batchSize = (harvestable.getStorageBatchLimit()==null ? 1 : harvestable.getStorageBatchLimit());
        Storage storage = harvestable.getStorage();
        setStorageConfig(storage);
        updateCounters = new InventoryRecordUpdateCounters();
        timingsStoringInventoryRecordSet = new HourlyPerformanceStats("Storing Inventory records", logger);
        timingsCreatingRecord = new HourlyPerformanceStats("Creating DOM for incoming record", logger);
        timingsCreatingRecord.setLogLevelForIntervals(Level.DEBUG);
        timingsTransformingRecord = new HourlyPerformanceStats("Transforming incoming record before storing", logger);
        timingsTransformingRecord.setLogLevelForIntervals(Level.DEBUG);
        failedRecordsController = new FailedRecordsController(logger, harvestable);
    }

    @Override
    public void moduleDatabaseStart(String database, Map<String, String> properties) {
    }

    @Override
    public void moduleDatabaseEnd() {

        if (!statusWritten) {
            String recordsSkippedMessage = (updateCounters.xmlBulkRecordsSkipped > 0 ? "Records skipped by date filter: " + updateCounters.xmlBulkRecordsSkipped : "");
            String instancesMessage = "Instances_processed/loaded/deleted(skipped)/failed:__" + updateCounters.instancesProcessed + "___" + updateCounters.instancesLoaded + "___" + updateCounters.instancesDeleted + "(" + updateCounters.instanceDeletesSkipped + ")___" + updateCounters.instancesFailed + "_";
            String holdingsRecordsMessage = "Holdings_records_processed/loaded/deleted(skipped)/failed:__" + updateCounters.holdingsRecordsProcessed + "___" + updateCounters.holdingsRecordsLoaded + "___" + updateCounters.holdingsRecordsDeleted + "(" + updateCounters.holdingsRecordDeletesSkipped + ")___" + updateCounters.holdingsRecordsFailed + "_";
            String itemsMessage = "Items_processed/loaded/deleted(skipped)/failed:__" + updateCounters.itemsProcessed + "___" + updateCounters.itemsLoaded + "___" + updateCounters.itemsDeleted + "(" + updateCounters.itemDeletesSkipped + ")___" + updateCounters.itemsFailed + "_";
            if (updateCounters.xmlBulkRecordsSkipped >0) logger.log(Level.INFO, recordsSkippedMessage);
            logger.log((updateCounters.instancesFailed>0 ? Level.WARN : Level.INFO), instancesMessage);
            logger.log((updateCounters.holdingsRecordsFailed>0 ? Level.WARN : Level.INFO), holdingsRecordsMessage);
            logger.log((updateCounters.itemsFailed>0 ? Level.WARN : Level.INFO), itemsMessage);

            failedRecordsController.writeLog();
            timingsCreatingRecord.writeLog();
            timingsTransformingRecord.writeLog();
            timingsStoringInventoryRecordSet.writeLog();
            harvestable.setMessage(recordsSkippedMessage + "  " + instancesMessage + " " + holdingsRecordsMessage + " " + itemsMessage );
            statusWritten=true;
        }
    }

    public void setStorageStatus(FolioStorageStatus status) {
        this.storageStatus = status;
    }

    protected void setFolioModuleConfigs() {
        inventoryUpsertPath = batchSize > 1 ? getConfig(INVENTORY_BATCH_UPSERT_PATH) : getConfig(INVENTORY_UPSERT_PATH);
        inventoryUpsertUrl = ( inventoryUpsertPath != null ? folioAddress + inventoryUpsertPath : null );
        inventoryDeleteUrl = (inventoryUpsertPath != null ? folioAddress + getConfig(INVENTORY_UPSERT_PATH) : null);
        logHistoryStoragePath = getConfig(LOG_HISTORY_STORAGE_PATH);
        logHistoryStorageUrl = (logHistoryStoragePath != null ? folioAddress + logHistoryStoragePath : null);
        logHistoryStorageUrlIsDefined = logHistoryStorageUrl != null;
    }

    @Override
    public String getStoragePath() {
        return inventoryUpsertPath;
    }


}