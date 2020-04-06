package com.indexdata.masterkey.localindices.harvest.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class RecordJSONImpl extends RecordImpl implements RecordJSON {

  private JSONObject json;

  @Override
  public JSONObject toJson() {
    return json;
  }

  @Override
  public void setJsonObject(JSONObject newJson) {
    this.json = newJson;
  }

  @Override
  public boolean isCollection() {
    return (this.json.containsKey("collection"));
  }

  @Override
  public Collection<Record> getSubRecords() {
    List<Record> list = new ArrayList<>();
    JSONArray records = (JSONArray)json.get("collection");
    if (records != null) {
      Iterator collectionIterator = records.iterator();
      while (collectionIterator.hasNext()) {
        RecordJSON record = new RecordJSONImpl();
        JSONObject next = (JSONObject) collectionIterator.next();
        record.setJsonObject(next);
        list.add(record);
      }
    }
    return list;
  }

  public void setIsDeleted(boolean isDeleted) {
    this.isDeleted = isDeleted;
  }
  @Override
  public boolean isDeleted() {
    return isDeleted;
  }

  private JSONObject getNextLevelRoot (JSONObject json) {
    JSONObject obj = new JSONObject();
    if (json.keySet().size()==1) {
      if (json.keySet().iterator().next() instanceof JSONObject) {
        obj = (JSONObject) (json.keySet().iterator().next());
      }
    }
    return obj;
  }

  @Override
  public String toString()  {
    return toJson().toJSONString();
  }

}
