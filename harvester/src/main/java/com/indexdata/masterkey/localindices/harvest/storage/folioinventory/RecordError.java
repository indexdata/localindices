package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

public interface RecordError {
    public String getMessageWithContext();
    public String getAdditionalContext();
    public String getErrorType();
    public String getServerMessage();
    public String getRecordType();
    public String getShortMessageForCounting();
    public String getTransaction();
    public String getEntity();
}