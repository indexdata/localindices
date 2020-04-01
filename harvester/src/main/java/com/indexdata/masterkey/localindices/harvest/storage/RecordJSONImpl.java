package com.indexdata.masterkey.localindices.harvest.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Level;
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

  @Override
  public boolean isDeleted() {

    if (json != null) {
      // try top-level
      if (json.containsKey("status") && json.get("status").toString().equalsIgnoreCase("deleted")) {
        return true;
      }
      // otherwise look in top-level record object
      if (json.containsKey("record")) {
        JSONObject record = ((JSONObject) json.get("record"));
        if (record.containsKey("status") && record.get("status").toString().equalsIgnoreCase("deleted")) {
          return true;
        }
      } else { // otherwise look in record object under another top-level "root" object
        JSONObject record = (JSONObject) getNextLevelRoot(json).get("record");
        if (record.containsKey("status") && record.get("status").toString().equalsIgnoreCase("deleted")) {
          return true;
        }
      }
    }
    return false;
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
