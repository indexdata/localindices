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

    protected static final String MARC_STORAGE_PATH = "marcStoragePath";
    protected static final String INVENTORY_UPSERT_PATH = "inventoryUpsertPath";

    public String marcStoragePath;
    public String inventoryUpsertPath;

    public String marcStorageUrl;
    public String inventoryUpsertUrl;
    public boolean marcStorageUrlIsDefined;

    public final Map<String,String> locationsToInstitutionsMap = new HashMap<>();
    public InventoryRecordUpdateCounters updateCounters;
    public HourlyPerformanceStats timingsStoringInventoryRecordSet;
    public HourlyPerformanceStats timingsCreatingRecord;
    public HourlyPerformanceStats timingsTransformingRecord;

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
        super(harvestable, logger);
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
        setLocationsToInstitutionsMap(getLocationsMap());
    }

    @Override
    public void moduleDatabaseEnd() {
        if (!statusWritten) {
            String recordsSkippedMessage = (updateCounters.xmlBulkRecordsSkipped > 0 ? "Records skipped by date filter: " + updateCounters.xmlBulkRecordsSkipped : "");
            String instancesMessage = "Instances_processed/loaded/deletions(signals)/failed:__" + updateCounters.instancesProcessed + "___" + updateCounters.instancesLoaded + "___" + updateCounters.instanceDeletions + "(" + updateCounters.instanceDeleteSignals + ")___" + updateCounters.instancesFailed + "_";
            String holdingsRecordsMessage = "Holdings_records_processed/loaded/deleted/failed:__" + updateCounters.holdingsRecordsProcessed + "___" + updateCounters.holdingsRecordsLoaded + "___" + updateCounters.holdingsRecordsDeleted + "___" + updateCounters.holdingsRecordsFailed + "_";
            String itemsMessage = "Items_processed/loaded/deleted/failed:__" + updateCounters.itemsProcessed + "___" + updateCounters.itemsLoaded + "___" + updateCounters.itemsDeleted + "___" + updateCounters.itemsFailed + "_";
            String sourceRecordsMessage = "Source_records_processed/loaded/deleted/failed:__" + updateCounters.sourceRecordsProcessed + "___" + updateCounters.sourceRecordsLoaded + "___" + updateCounters.sourceRecordsDeleted + "___" + updateCounters.sourceRecordsFailed + "_";
            if (updateCounters.xmlBulkRecordsSkipped >0) logger.log(Level.INFO, recordsSkippedMessage);
            logger.log((updateCounters.instancesFailed>0 ? Level.WARN : Level.INFO), instancesMessage);
            logger.log((updateCounters.holdingsRecordsFailed>0 ? Level.WARN : Level.INFO), holdingsRecordsMessage);
            logger.log((updateCounters.itemsFailed>0 ? Level.WARN : Level.INFO), itemsMessage);
            logger.log((updateCounters.sourceRecordsFailed>0 ? Level.WARN : Level.INFO), sourceRecordsMessage);

            failedRecordsController.writeLog();
            timingsCreatingRecord.writeLog();
            timingsTransformingRecord.writeLog();
            timingsStoringInventoryRecordSet.writeLog();
            harvestable.setMessage(recordsSkippedMessage + "  " + instancesMessage + " " + holdingsRecordsMessage + " " + itemsMessage + " " + sourceRecordsMessage);
            statusWritten=true;
        }

    }

    /**
     * Retrieve locations-to-institutions mappings from Inventory storage
     * Used for holdings/items deletion logic.
     */
    private Map<String,String> getLocationsMap() throws StorageException {
        try {
            Map<String,String> locationsToInstitutions = new HashMap<>();
            String url = String.format("%s", folioAddress + "locations?limit=9999");
            HttpGet httpGet = new HttpGet(url);
            setHeaders(httpGet, "application/json");
            CloseableHttpResponse response = folioClient.execute(httpGet);
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

    public void setStorageStatus(FolioStorageStatus status) {
        this.storageStatus = status;
    }

    public void setLocationsToInstitutionsMap(Map<String,String> locationsToInstitutions) {
        this.locationsToInstitutionsMap.putAll(locationsToInstitutions);
    }

    protected void setFolioModuleConfigs() {
        inventoryUpsertPath = getConfig(INVENTORY_UPSERT_PATH);
        inventoryUpsertUrl = (inventoryUpsertPath != null ? folioAddress + inventoryUpsertPath : null);
        marcStoragePath = getConfig(MARC_STORAGE_PATH);
        marcStorageUrl = (marcStoragePath != null ? folioAddress + marcStoragePath : null);
        marcStorageUrlIsDefined = marcStorageUrl != null;
    }

    @Override
    public String getStoragePath() {
        return inventoryUpsertPath;
    }


}