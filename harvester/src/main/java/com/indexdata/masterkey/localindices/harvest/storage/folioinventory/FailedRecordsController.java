package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

/**
 * Creates and controls access to the directory for storing failed records,
 * writes files to it, counts files and errors for reporting and
 * restricts the maximum number of files written as configured.
 *
 */
public class FailedRecordsController {
    protected int maxFailedRecordsSavedThisRun = 100;
    protected int maxFailedRecordsSavedTotal = 450;
    protected RecordFailureCounters recordFailureCounters;
    protected static final String HARVESTER_LOG_DIR = "/var/log/masterkey/harvester/";
    protected static final String FAILED_RECORDS_DIR = "failed-records/";
    Path failedRecordsDirectory;
    protected StorageJobLogger logger;
    Long jobId;
    protected boolean directoryReady;
    int initialNumberOfFiles = 0;
    int calculatedNumberOfFiles = 0;

    public FailedRecordsController(StorageJobLogger logger, Long jobId) {
        logger.debug("Initializes setup for saving failed records to directory");
        this.jobId = jobId;
        this.recordFailureCounters = new RecordFailureCounters();
        this.logger = logger;
        this.failedRecordsDirectory = Paths.get(HARVESTER_LOG_DIR, FAILED_RECORDS_DIR, jobId.toString());

        try {
            Files.createDirectories(failedRecordsDirectory);
            directoryReady = true;
            initialNumberOfFiles = countFiles(failedRecordsDirectory);
            calculatedNumberOfFiles = initialNumberOfFiles;
            this.logger.info("There are " + initialNumberOfFiles + " files in the job's failed-records directory at the beginning of this run");
        } catch (IOException e) {
            directoryReady = false;
            this.logger.error("Could not initialize directory for storing failed records. Will not save any failed records");
        }
    }

    @SuppressWarnings("unused")
    private int countFiles (Path dir) {
        int count = 0;
        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir);
            for (Path path : directoryStream) {
                count++;
            }
        } catch (IOException e) {
            logger.error("Error counting files in failed records directory. " + e.getMessage());
        }

        return count;
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

    public void saveFailedRecord(RecordWithErrors failedRecord) {
        if (directoryReady) {
            try {
                byte[] xml = failedRecord.createFailedRecordXml();
                if (xml == null) {
                    logger.error("Failed record XML for saving was not created for ID " + failedRecord.record.getId()+". Skipping save. ");
                } else {
                    if (reachingMaxFailedRecordsSavedThisRunNext()) {
                        logger.info("Will stop saving failed records after this. Maximum number of error files for this run reached ("+ maxFailedRecordsSavedThisRun+").");
                    }
                    if (reachingMaxFailedRecordsSavedTotalNext()) {
                        logger.info("Will stop saving failed records after this. Maximum total number of error files for this job reached ("+ maxFailedRecordsSavedTotal+").");
                    }
                    if (! reachedMaxFailedRecordsSaved()) {
                        Files.write(failedRecord.failedRecordFilePath, xml);
                        recordFailureCounters.countFailedRecordsSaved(failedRecord);
                        calculatedNumberOfFiles++;
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

    public int getErrorsByErrorMessage(String message) {
        return getCounters().errorsByErrorMessage.get(message);
    }

    public int incrementErrorCount (RecordError error) {
        return recordFailureCounters.incrementErrorCount(error);
    }

    public boolean reachingMaxFailedRecordsSavedTotalNext() {
        return calculatedNumberOfFiles == maxFailedRecordsSavedTotal - 1;
    }

    public boolean reachedMaxFailedRecordsSavedTotal () {
        return calculatedNumberOfFiles >= maxFailedRecordsSavedTotal;
    }

    public boolean reachingMaxFailedRecordsSavedThisRunNext () {
        return recordFailureCounters.failedRecordsSaved == maxFailedRecordsSavedThisRun - 1;
    }

    public boolean reachedMaxFailedRecordsSavedThisRun () {
        return recordFailureCounters.failedRecordsSaved >= maxFailedRecordsSavedThisRun;
    }

    public boolean reachedMaxFailedRecordsSaved () {
        return reachedMaxFailedRecordsSavedThisRun() || reachedMaxFailedRecordsSavedTotal();
    }
}