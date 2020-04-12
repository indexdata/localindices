package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

/**
 * Creates and controls access to directory for storing failed records,
 * writes files to it, counts files and errors for reporting and
 * restricts the maximum number of files written as configured.
 *
 */
public class FailedRecordsController {
    protected int maxFailedRecordsSavedThisRun = 100;
    protected int maxFailedRecordsSavedTotal = 1000;
    protected RecordFailureCounters recordFailureCounters;
    protected static final String HARVESTER_LOG_DIR = "/var/log/masterkey/harvester/";
    protected static final String FAILED_RECORDS_DIR = "failed-records/";
    Path failedRecordsDirectory;
    protected StorageJobLogger logger;
    Long jobId;
    protected boolean directoryReady;

    public FailedRecordsController(StorageJobLogger logger, Long jobId) {
        this.jobId = jobId;
        this.recordFailureCounters = new RecordFailureCounters();
        this.logger = logger;
        this.failedRecordsDirectory = Paths.get(HARVESTER_LOG_DIR, FAILED_RECORDS_DIR, jobId.toString());
        try {
            Files.createDirectories(failedRecordsDirectory);
            directoryReady = true;
        } catch (IOException e) {
            directoryReady = false;
            logger.error("Could not initialize directory for storing failed records. Will not save any failed records");
        }
    }

    public String getFailedRecordsDirectory () {
        if (directoryReady) {
            return HARVESTER_LOG_DIR + FAILED_RECORDS_DIR + jobId.toString();
        } else {
            return null;
        }
    }

    public String getFailedRecordPath (String fileName) {
        if (directoryReady) {
            return getFailedRecordsDirectory() + "/" + fileName;
        } else {
            return null;
        }
    }

    public void writeLog() {
        for (String key : recordFailureCounters.errorsByErrorMessage.keySet()) {
            logger.info(String.format("%d records failed with %s", recordFailureCounters.errorsByErrorMessage.get(key),key));
        }
    }

    public void saveFailedRecord(RecordErrors failedRecord) {
        if (directoryReady) {
            try {
                byte[] xml = failedRecord.createFailedRecordXml();
                if (xml == null) {
                    logger.error("Failed record XML for saving was not created for ID " + failedRecord.record.getId()+". Skipping save. ");
                } else {
                    if (reachingMaxFailedRecordsSavedThisRunNext()) {
                        logger.info("Will stop saving failed records after this. Maximum number of error files reached ("+ maxFailedRecordsSavedThisRun+").");
                    }
                    if (! reachedMaxFailedRecordsSavedThisRunYet()) {
                        Files.write(failedRecord.failedRecordFilePath, xml);
                        recordFailureCounters.countFailedRecordsSaved(failedRecord);
                    } else {
                        logger.debug("Reached max failed records to save, skipping save.");
                    }
                }
            } catch (IOException ioe) {
                logger.error("IOException when attempting to save failed record to failed-records directory: "+ ioe.getMessage() + " " + ioe.getCause().getMessage());
            }
        } else {
            logger.error("Skipped saving failed record with ID [" + failedRecord.record.getId() + "] (directory not ready)");
        }
    }

    public  RecordFailureCounters getCounters () {
        return this.recordFailureCounters;
    }

    public boolean reachingMaxFailedRecordsSavedThisRunNext () {
        return recordFailureCounters.failedRecordsSaved == maxFailedRecordsSavedThisRun-1;
    }

    public boolean reachedMaxFailedRecordsSavedThisRunYet () {
        return recordFailureCounters.failedRecordsSaved >= maxFailedRecordsSavedThisRun;
    }
}