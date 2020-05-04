package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

import org.apache.commons.io.FileUtils;

/**
 * Creates and controls access to the directory for storing failed records,
 * writes files to it, counts files and errors for reporting and
 * restricts the maximum number of files written as configured.
 *
 */
public class FailedRecordsController {
    // Configuration - requires implementation in Harvestable config
    protected int maxFailedRecordFilesThisRun = 100;
    protected int maxFailedRecordFilesTotal = 450;
    protected enum StoreMode {NO_STORE, CLEAN_DIRECTORY, CREATE_OVERWRITE, ADD_ALL};
    protected StoreMode mode = StoreMode.CLEAN_DIRECTORY;
    protected static final String HARVESTER_LOG_DIR = "/var/log/masterkey/harvester/";
    protected static final String FAILED_RECORDS_DIR = "failed-records/";
    // End of configuration

    protected StorageJobLogger logger;
    Long jobId;
    protected boolean directoryReady = false;
    protected RecordFailureCounters recordFailureCounters;
    int initialNumberOfFiles = 0;
    int calculatedNumberOfFiles = 0;

    public FailedRecordsController(StorageJobLogger logger, Long jobId) {
        this.jobId = jobId;
        this.recordFailureCounters = new RecordFailureCounters();
        this.logger = logger;
    }

    public String getMode() {
        return mode.name();
    }

    /**
     * Creates failed-records directory for the job if it doesn't exists, delete all files
     * in the directory if mode is CLEAN_DIRECTORY, count the files in it in order to observe
     * max failed-record files settings for the job.
     *
     * @param logger
     * @param jobId
     */
    @SuppressWarnings("unused")  // Requires implementation in Harvestable config
    private void prepareFailedRecordsDirectory(StorageJobLogger logger, Long jobId) {
        Path failedRecordsDirectory = Paths.get(HARVESTER_LOG_DIR, FAILED_RECORDS_DIR, jobId.toString());
        try {
            Files.createDirectories(failedRecordsDirectory);
            if (mode==StoreMode.CLEAN_DIRECTORY) {
                try {
                    FileUtils.cleanDirectory(failedRecordsDirectory.toFile());
                    directoryReady = true;
                } catch (IOException ioe) {
                    logger.error("Attempt to clean up failed-records directory failed");
                }
            } else {
                directoryReady = true;
            }
            initialNumberOfFiles = countFiles(failedRecordsDirectory);
            calculatedNumberOfFiles = initialNumberOfFiles;
            logger.info("Initialized failed-records controller (mode " + getMode() + ")");
            logger.info("There are " + initialNumberOfFiles + " file(s) in the job's failed-records directory at the beginning of this run");
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
            for (Path path : directoryStream) count++;
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

    /**
     * Writes statistics to job log, counting errors by type
     */
    public void writeLog() {
        for (String key : recordFailureCounters.errorsByErrorMessage.keySet()) {
            logger.info(String.format("%d records failed with %s", recordFailureCounters.errorsByErrorMessage.get(key),key));
        }
    }

    /**
     * Saves a failed record to the failed-records directory, except if mode is NO_STORE
     * or max files has been reached for the directory and job run.
     *
     * @param failedRecord the record to save
     */
    public void saveFailedRecord(RecordWithErrors failedRecord) {
        if (mode!=StoreMode.NO_STORE) {
            if (directoryReady) {
                if (reachingMaxFailedRecordsSavedThisRunNext()) {
                    logger.info("Will stop saving failed records after this. Maximum number of error files for this run reached ("+ maxFailedRecordFilesThisRun+").");
                }
                if (reachingMaxFailedRecordsSavedTotalNext()) {
                    logger.info("Will stop saving failed records after this. Maximum total number of error files for this job reached ("+ maxFailedRecordFilesTotal+").");
                }
                if (! reachedMaxFailedRecordsSaved()) {
                    saveToFile(failedRecord);
                } else {
                    logger.debug("Reached max failed records to save, skipping save.");
                }
            } else {
                logger.error("Skipped saving failed record with ID [" + failedRecord.getRecordIdentifier() + "] (directory not ready)");
            }
        }
    }

    /**
     *
     * @param failedRecord
     * @param xml
     * @throws IOException
     */
    private void saveToFile(RecordWithErrors failedRecord) {
        try {
            byte[] xml = failedRecord.createFailedRecordXml();
            Path filePath = calculateFilePath(failedRecord);
            if (xml != null && filePath != null) {
                Files.write(filePath, xml);
                recordFailureCounters.countFailedRecordsSaved(failedRecord);
                calculatedNumberOfFiles++;
            } else if (xml==null) {
                logger.error("Failed-record XML was not successfully created - cannot save it");
            } else if (filePath == null) {
                logger.error("Failed-record file path was not successfully calculated - cannot save failed record");
            }
        } catch (IOException ioe) {
            logger.error("IOException when attempting to save failed record to failed-records directory: "+ ioe.getMessage() + " " + ioe.getCause().getMessage());
        }
    }

    /**
     * Creates simple file path, except if mode is ADD_ALL, in which case it will
     * avoid overwriting existing files by appending a sequence number (up to 9)
     * to the simple file name.
     * @param failedRecord the record to find file name for
     * @return
     */
    private Path calculateFilePath(RecordWithErrors failedRecord) {
        Path filePath = Paths.get(getFailedRecordPath(getFileName(failedRecord)));
        if (mode == StoreMode.ADD_ALL) {
            int i = 1;
            while (Files.exists(filePath)) {
                filePath = Paths.get(getFailedRecordPath(getFileName(failedRecord,i)));
                i++;
                if (i>9) break;
            }
        }
        return filePath;
    }

    private String getFileName (RecordWithErrors failedRecord) {
        String filename = ((String) failedRecord.getRecordIdentifier()) + ".xml";
        return filename;
    }

    private String getFileName (RecordWithErrors failedRecord, int version) {
        return String.format("%s-%d.xml", failedRecord.getRecordIdentifier(), version);
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
        return calculatedNumberOfFiles == maxFailedRecordFilesTotal - 1;
    }

    public boolean reachedMaxFailedRecordsSavedTotal () {
        return calculatedNumberOfFiles >= maxFailedRecordFilesTotal;
    }

    public boolean reachingMaxFailedRecordsSavedThisRunNext () {
        return recordFailureCounters.failedRecordsSaved == maxFailedRecordFilesThisRun - 1;
    }

    public boolean reachedMaxFailedRecordsSavedThisRun () {
        return recordFailureCounters.failedRecordsSaved >= maxFailedRecordFilesThisRun;
    }

    public boolean reachedMaxFailedRecordsSaved () {
        return reachedMaxFailedRecordsSavedThisRun() || reachedMaxFailedRecordsSavedTotal();
    }
}