package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
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
  /**
   * Extend API to also support a Record
   */
  void delete(String id);

  void setLogger(StorageJobLogger logger);
  
  StorageStatus getStatus() throws StatusNotImplemented;
  
  // SAX ContentHandler
  DatabaseContenthandler getContentHandler();
  
  // Some storages needs to close resources/shutdown connections. 
  void shutdown() throws IOException; 
}
