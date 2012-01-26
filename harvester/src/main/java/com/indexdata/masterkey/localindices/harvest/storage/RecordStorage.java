package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

public interface RecordStorage extends HarvestStorage {

  void databaseStart(String database, Map<String, String> properties);

  void databaseEnd();

  void add(Map<String, Collection<Serializable>> keyValues);

  void add(Record record);

  Record get(String id);

  // void remove(Collection<String> ids);
  void delete(String id);

  void setLogger(StorageJobLogger logger);
}
