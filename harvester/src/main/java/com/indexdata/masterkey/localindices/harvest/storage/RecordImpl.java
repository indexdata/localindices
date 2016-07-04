package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RecordImpl implements Record {

  Map<String, Collection<Serializable>> valueMap;
  String id;
  String database;
  byte[] content;
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
  public byte[] getOriginalContent() {
    return content;
  }
  
  public void setOriginalContent(byte[] content) {
    this.content = content;
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
    StringBuffer record = new StringBuffer("Record[");
    record.append("id=").append(id).append(", ");
    // HACK: We don't know the "id" field
    if (valueMap.containsKey("id"))
      record.append("get('id')= " + valueMap.get("id").toString()).append(", ");
    record.append("database=").append(database);
    for (String key : valueMap.keySet()) {
      record.append(", ");
      // TODO serialize the Collection.. 
      record.append(key).append(" => {").append(valueMap.get(key)).append("}");
    }
    record.append("]");
    return record.toString();
  }

  @Override
  public boolean isCollection() {
    return false;
  }
  
  @Override
  public Collection<Record> getSubRecords() {
    return null;
  }
}
