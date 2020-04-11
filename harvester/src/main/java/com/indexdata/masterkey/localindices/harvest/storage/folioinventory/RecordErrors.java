/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.util.ArrayList;
import java.util.List;
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

  RecordErrors(RecordJSON recordJson, Map<String,Integer> errorCounts) {
    this.record = recordJson;
    this.errorCounts = errorCounts;
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

  void reportAndThrowError(RecordError error, StorageJobLogger logger, Level level) throws InventoryUpdateException {
    reportAndThrowError(error, logger, level, null);
  }

  void reportAndThrowError(RecordError error, StorageJobLogger logger, Level level, Throwable exception) throws InventoryUpdateException {
    addError(error);
    int count = countError(error);
    if (count <= 10) {
      logger.log(level, error.toString());
    } else if (count>10 && count < 100) {
      logger.log(level, error.briefMessage());
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

}
