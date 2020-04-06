package com.indexdata.masterkey.localindices.harvest.storage;

import org.json.simple.JSONObject;

public interface RecordJSON extends Record {
    JSONObject toJson();
    void setJsonObject(JSONObject newJson);
    void setIsDeleted(boolean isDeleted);
}
