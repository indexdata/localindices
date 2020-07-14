package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

public interface RecordError {
    public String getMessage ();
    public String getErrorContext();
    public String getType();
    public String getBriefMessage();
    public String getStorageEntity();
    public String getCountingKey();
}