package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

public interface RecordStorage  {

  /**
   * Opens the storage and prepares output stream for writes.
   *
   * @throws java.io.IOException
   */
  public void begin() throws IOException;

  /**
   * Commits the current harvest and closes output stream.
   *
   * @throws java.io.IOException
   */
  public void commit() throws IOException;

  /**
   * Rolls back the current write and closes the output stream.
   *
   * @throws java.io.IOException
   */
  public void rollback() throws IOException;

  /**
   * Purges all data written so far (drops the whole storage).
   *
   * @throws java.io.IOException
   */
  public void purge(boolean commit) throws IOException;

  //public void purgeCommit() throws IOException;


  /**
   * Set/get a flag that indicates the overwrite mode Normally a storage is in
   * append mode, so new transactions (begin-write-commit) are appended to the
   * existing ones. But in overwrite mode, we remove all the old stuff when
   * doing the commit. Useful for things that need to be harvested all over
   * again, like bulk uploads.
   */
  public void setOverwriteMode(boolean mode);

  public boolean getOverwriteMode();


  public void setHarvestable(Harvestable harvestable);

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

  void delete(Record record);

  void setLogger(StorageJobLogger logger);

  StorageStatus getStatus() throws StatusNotImplemented;

  // SAX ContentHandler
  DatabaseContenthandler getContentHandler();

  // Some storages needs to close resources/shutdown connections.
  void shutdown() throws IOException;

  /**
   * Sets number of record stored within a single batch.
   *
   * @param limit
   */
  void setBatchLimit(int limit);
}
