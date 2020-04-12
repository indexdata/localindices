package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

import java.util.HashMap;
import java.util.Map;

public class RecordFailureCounters {
    protected final Map<String,Integer> errorsByErrorMessage = new HashMap<String,Integer>();
    protected final Map<String,Integer> failedRecordsSavedByErrorMessage = new HashMap<String,Integer>();
    protected int failedRecordsSaved = 0;

     // TODO: maybe concatenate messages in case of multiple errors
    public void countFailedRecordsSaved (RecordErrors record) {
        failedRecordsSaved++;
        String message = record.errors.get(0).getMessage();
        if (failedRecordsSavedByErrorMessage.containsKey(message)) {
            failedRecordsSavedByErrorMessage.put(message,failedRecordsSavedByErrorMessage.get(message)+1);
        } else {
            failedRecordsSavedByErrorMessage.put(message, 1);
        }
    }

    public int countErrors(RecordError error) {
        String message = error.getMessage();
        if (errorsByErrorMessage.containsKey(message)) {
            errorsByErrorMessage.put(message,errorsByErrorMessage.get(message)+1);
        } else {
            errorsByErrorMessage.put(message, 1);
        }
        return errorsByErrorMessage.get(message);
    }
}