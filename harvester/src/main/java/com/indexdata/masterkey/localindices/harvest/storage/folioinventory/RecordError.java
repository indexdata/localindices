package com.indexdata.masterkey.localindices.harvest.storage.folioinventory;

public interface RecordError {
    public String getMessage ();
    public String getLabel();
    public String getType();
    public String getBriefMessage();
    public String getStorageEntity();
}