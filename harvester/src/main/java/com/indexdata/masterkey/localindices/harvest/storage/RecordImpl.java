package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RecordImpl implements Record {

  Map<String, Collection<Serializable>> valueMap;
  String id;
  String database;
  boolean isDeleted;

  public RecordImpl() {
    valueMap = new HashMap<String, Collection<Serializable>>();
  }

  public RecordImpl(Map<String, Collection<Serializable>> values) {
    valueMap = values;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getDatabase() {
    return database;
  }

  @Override
  public Map<String, Collection<Serializable>> getValues() {
    return valueMap;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  @Override
  public boolean isDeleted() {
    // TODO Auto-generated method stub
    return false;
  }

  public void setDeleted(boolean isDeleted) {
    this.isDeleted = isDeleted;
  }

  public String toString() {
    String record = "Record[";
    record.concat("id=" + id + ", ");
    record.concat("database=" + database);
    boolean first = true; 
    for (String key : valueMap.keySet()) {
      if (!first) {
	record.concat(", ");
	first = false; 
      }
      // TODO serialize the Collection.. 
      record.concat(key + "=> {" + valueMap.get(key) + "}");
    }
    record += "]";
    return record;
  }
}
