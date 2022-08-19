package com.indexdata.masterkey.localindices.harvest.storage.folio;


import java.util.HashMap;
import java.util.Map;

public class RecordFailureCounters {

    protected final Map<String,Integer> errorsByShortErrorMessage = new HashMap<>();
    protected final Map<String,Integer> failedRecordsSavedByErrorMessage = new HashMap<>();
    protected int failedRecordsSaved = 0;

     // TODO: maybe concatenate messages in case of multiple errors
    public void countFailedRecordsSaved (RecordWithErrors record) {
        if (record.errors.size()>0) {
            failedRecordsSaved++;
            String message = record.errors.get(0).getMessageWithContext();
            if (failedRecordsSavedByErrorMessage.containsKey(message)) {
                failedRecordsSavedByErrorMessage.put(message,
                        failedRecordsSavedByErrorMessage.get(message) + 1);
            } else {
                failedRecordsSavedByErrorMessage.put(message, 1);
            }
            record.logger.info("RecordFailureCounters was asked to count errors but error list had none.");
        }
    }

    public int incrementErrorCount(RecordError error) {
        String errorKey = error.getShortMessageForCounting();
        if (errorsByShortErrorMessage.containsKey(errorKey)) {
            errorsByShortErrorMessage.put(errorKey, errorsByShortErrorMessage.get(errorKey)+1);
        } else {
            errorsByShortErrorMessage.put(errorKey, 1);
        }
        return errorsByShortErrorMessage.get(errorKey);
    }
}