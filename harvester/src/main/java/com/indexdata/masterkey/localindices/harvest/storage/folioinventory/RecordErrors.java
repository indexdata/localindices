/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Level;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.RecordJSON;

/**
 * Holds errors encountered in the processing of one Inventory record set (the
 * Inventory records derived from one incoming bibliographic record)
 *
 * @author ne
 */
public class RecordErrors {

  RecordJSON record;
  List<RecordError> errors = new ArrayList<RecordError>();
  Map<String,Integer> errorCounts;
  private static final String HARVESTER_LOG_DIR = "/var/log/masterkey/harvester/";
  private static final String FAILED_RECORDS_DIR = "failed-records/";
  Long jobId;
  Path failedRecordsDirectory = null;
  Path failedRecordFilePath = null;
  TransformedRecord recordProxy;
  StorageJobLogger logger;


  RecordErrors(RecordJSON recordJson, Map<String,Integer> errorCounts, Long jobId, StorageJobLogger logger) {
    this.record = recordJson;
    this.errorCounts = errorCounts;
    this.jobId = jobId;
    this.logger = logger;
    this.recordProxy = new TransformedRecord(recordJson.toJson(),logger);

    try {
      this.failedRecordsDirectory = Paths.get(HARVESTER_LOG_DIR, FAILED_RECORDS_DIR, jobId.toString());
      Files.createDirectories(failedRecordsDirectory);
      String fileName = recordProxy.getLocalIdentifier() + "-" + timestamp() + ".xml";
      this.failedRecordFilePath = Paths.get(HARVESTER_LOG_DIR, FAILED_RECORDS_DIR, jobId.toString(), fileName);
    } catch (IOException ioe) {
      logger.error("IOException when attempting to create directory for failed records - will not store failed records: " + ioe.getMessage());
    }

  }

  private String timestamp () {
    LocalDateTime now = LocalDateTime.now();
    String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.getDefault()));
    return timestamp;
  }

  void addResponseError(HttpRecordError error) {
    errors.add(error);
  }

  void addException(ExceptionRecordError error) {
    errors.add(error);
  }

  void addError(RecordError error) {
    if (error instanceof HttpRecordError) {
      addResponseError((HttpRecordError)error);
    } else if (error instanceof ExceptionRecordError) {
      addException((ExceptionRecordError) error);
    }
  }

  void reportAndThrowError(RecordError error, Level logLevel) throws InventoryUpdateException {
    reportAndThrowError(error, logLevel, null);
  }

  void reportAndThrowError(RecordError error, Level logLevel, Throwable exception) throws InventoryUpdateException {
    addError(error);
    int count = countError(error);
    if (count <= 10) {
      logger.log(logLevel, error.toString());
    } else if (count>10 && count < 100) {
      logger.log(logLevel, error.briefMessage());
    } else if (count % 100 == 0) {
      logger.error(String.format("%d records failed with %s", errorCounts.get(error.briefMessage()), error.briefMessage()));
    }
    if (exception != null) {
      throw new InventoryUpdateException(error.toString(),exception);
    } else {
      throw new InventoryUpdateException(error.toString());
    }
  }

  void addResponseError(int status, String reason, String response, String message, String entity) {
    errors.add(new HttpRecordError(status, reason, response, message, entity));
  }

  void addResponseError(int status, String reason, String response, String message) {
    errors.add(new HttpRecordError(status, reason, response, message));
  }

  void addExceptionError(Exception e, String message, String entity) {
    errors.add(new ExceptionRecordError(e, message, entity));
  }

  void addExceptionError(Exception e, String message) {
    errors.add(new ExceptionRecordError(e, message, ""));
  }

  boolean hasErrors () {
    return errors.size()>0;
  }

  int numberOfErrors() {
    return errors.size();
  }

  /**
   * Log full error messages for errors that occurred for less than 10 records in the job so far
   * and a brief error message with a total count for every 100 records with only that error thereafter.
   *
   * @param logger
   * @param counters
   */
  void writeErrorsLog(StorageJobLogger logger, RecordUpdateCounts counters) {
    if (hasErrors()) {
      int i=0;
      for (RecordError error : errors) {
        i++;
        int occurrences = counters.exceptionCounts.get(error.briefMessage());
        if (occurrences < 10) {
          if (i==1) logger.error("Error" + (numberOfErrors() > 1 ? "s" : "") + " updating Inventory with  " + record.toJson().toJSONString());
          logger.error("#" + i + " " + error.toString());
        } else if (occurrences % 100 == 0) {
          logger.error(occurrences + " records have failed with " + error.briefMessage());
        }
      }
    }
  }

  private int countError(RecordError error) {
    if (errorCounts.containsKey(error.briefMessage())) {
      errorCounts.put(error.briefMessage(), errorCounts.get(error.briefMessage()) + 1);
    } else {
      errorCounts.put(error.briefMessage(), 1);
    }
    return errorCounts.get(error.briefMessage());
  }

  public void logFailedRecord () {
    if (failedRecordFilePath != null) {
      try {
        Path failedRecordFile = Files.createFile(failedRecordFilePath);
        Files.write(failedRecordFile, record.getOriginalContent(), StandardOpenOption.CREATE );
      } catch (IOException ioe) {
        logger.error("IOException when attempting to save failed record to failed-records directory: "+ ioe.getMessage());
      }
    } else {
      logger.debug("Attempted to save failed record but directory for failed records not properly created/defined.");
    }
  }


}
