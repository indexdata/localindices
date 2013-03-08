package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.storage.StorageStatus.TransactionState;

public class SolrRecordStorage extends SolrStorage implements RecordStorage {
  Collection<String> deleteIds = new LinkedList<String>();
  private String idField = "id";
  private String databaseField = "database";
  private String database;
  @SuppressWarnings("unused")
  private Map<String, String> databaseProperties;
  //protected int added;
  //protected int deleted;
  //private boolean committed;
  private Date transactionId;
  private boolean delayedPurge = true;
  private boolean waitSearcher = false;
  private boolean isPurged;
  private String transactionIdField = "solrLastModified";
  SolrStorageStatus storageStatus;

  public SolrRecordStorage(String url, Harvestable harvestable) {
    super(url, harvestable);
    try {
      storageStatus = new SolrStorageStatus(url, databaseField + ":" + harvestable.getId());
    } catch (MalformedURLException e) {
      // This would already have been thrown in super. 
      e.printStackTrace();
    }

  }

  @Override
  synchronized public void begin() throws IOException {
    super.begin();
    transactionId = new Date();
    database = harvestable.getId().toString();
    storageStatus.setTransactionState(TransactionState.InTransaction);
  }

  @Override
  synchronized public void commit() throws IOException {
    try {
      if (delayedPurge && isPurged) {
	purgeByTransactionId(false);
      }
      logger.info("Committing " + storageStatus.getOutstandingAdds() + " added and " + storageStatus.getOutstandingDeletes() + " deleted records to database " + database);
      // Testing waitFlush=true, waitSearcher=false. Not good for indexes with searchers, but better for crawlers. 
      UpdateResponse response = server.commit(true, waitSearcher);
      if (response.getStatus() != 0)
	logger.error("Error while COMMITING records.");
      else	
	storageStatus.setTransactionState(TransactionState.Committed);
    } catch (SolrServerException e) {
      logger.error("Commit failed when adding " + storageStatus.getOutstandingAdds() + " and deleting " + storageStatus.getOutstandingDeletes() + " to database " + database, e);
      e.getStackTrace();
      throw new IOException("Commit failed: " + e.getMessage(), e);
    }
  }

  @Override
  synchronized public void rollback() throws IOException {
      purgeByTransactionId(true);
  }

  @Override
  synchronized public void purge(boolean commit) throws IOException {
    try {
      if (database == null) {
	logger.error("purge called before begin.");
	begin();
      }
      if (!delayedPurge || commit == true) {
	String query = databaseField + ":" + database;
	UpdateResponse response = server.deleteByQuery(query);
	logger.info("UpdateResponse on delete(" + query + "): " + response.getStatus() + " " + response.getResponse());
	if (commit) {
	  response = server.commit();
	  logger.info("UpdateResponse on commit delete: " + response.getStatus() + " " + response.getResponse());
	}
      }
      else {
	logger.info("Performing purge later.");
	isPurged = true; 
      }
    } catch (SolrServerException e) {
      e.printStackTrace();
      throw new IOException("Error purging database (" + database + ")", e);
    }
  }

  /** 
   * purgeByTransactionId can delete all records either with or without the current transaction id. 
   * rollback would call this to delete all records with the current transaction id. 
   * TODO: Verify that this works on non-committed records... 
   * Delayed purge would delete all records without the id, thus simulating a purge of all other records.  
   * 
   * @param hasId
   * @throws IOException
   */
  public void purgeByTransactionId(boolean hasId) throws IOException {
    try {
      if (database == null) {
	logger.error("purge called before begin.");
	// throw NotInTransaction
      }
      String hasIdString = (hasId ? "" : "!");
      String query = databaseField + ":" + database + " AND " + hasIdString + transactionIdField  + ":" + transactionId.getTime();
      UpdateResponse response = server.deleteByQuery(query);
      logger.info("UpdateResponse on delete (" + query + "): " + response.getStatus() + " " + response.getResponse());
    } catch (SolrServerException e) {
      e.printStackTrace();
      throw new IOException("Error purging database (" + database + ")", e);
    }
  }

  @Override
  public OutputStream getOutputStream() {
    throw new RuntimeException("Not supporting OutputStream interface.");
  }

  @Override
  public void databaseStart(String database, Map<String, String> properties) {
    databaseProperties = properties;
    if (this.database != null && !this.database.equals(database)) { 
      logger.debug("Current Database " + this.database + ". New database: " + database);
    }
    this.database = database;
  }

  // @Deprecated createDocument Use Record method
  protected SolrInputDocument createDocument(Map<String, Collection<Serializable>> keyValues) {
    SolrInputDocument document = new SolrInputDocument();
    for (String key : keyValues.keySet()) {
      for (Serializable value : keyValues.get(key)) {
	document.addField(key, value);
      }
    }
    return document;
  }

  protected SolrInputDocument createDocument(Record record) {
    SolrInputDocument document = createDocument(record.getValues());
    if (idField != null)
      document.setField(idField, record.getId());

    if (databaseField != null)
      document.setField(databaseField, database);
    
    if (transactionId  != null)
      document.setField(transactionIdField, transactionId.getTime());

    return document;
  }

  protected Record createRecord(SolrDocument doc) {
    Record record = null;
    return record;
  }

  private void add(SolrInputDocument document) {
    try {
      UpdateResponse updateResponse = server.add(document);
      if (updateResponse.getStatus() != 0)
	logger.error("Add record (" + document + ") failed. Status: " + updateResponse.getStatus());
      else
	storageStatus.incrementAdd(1);
    } catch (SolrServerException e) {
      logger.error("SolrServer Exception on add: " + e.getMessage(), e);
      e.printStackTrace();
    } catch (IOException e) {
      logger.error("IO Exception on add: " + e.getMessage(), e);
      e.printStackTrace();
    }

  }

  @Override
  synchronized public void add(Map<String, Collection<Serializable>> keyValues) {
    add(createDocument(keyValues));
  }

  @Override
  synchronized public void add(Record record) {
    add(createDocument(record));
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private Collection<Serializable> getValueAsCollection(Object obj) {
    if (obj instanceof Collection)
      return (Collection) obj;
    Collection<Serializable> collection = new LinkedList<Serializable>();
    if (obj instanceof Serializable)
      collection.add((Serializable) obj);
    return collection;
  }

  private Map<String, Collection<Serializable>> createMap(SolrDocument solrDocument) {
    Map<String, Collection<Serializable>> map = new HashMap<String, Collection<Serializable>>();
    for (String field : solrDocument.getFieldNames()) {
      map.put(field, getValueAsCollection(solrDocument.getFieldValue(field)));
    }
    return map;
  }

  @Override
  public Record get(String id) {
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set(idField, id);
    params.set(databaseField, database);

    try {
      QueryResponse response = server.query(params);
      if (response.getStatus() == 200) {
	SolrDocumentList list = response.getResults();
	if (list.size() == 1) {
	  RecordImpl record = new RecordImpl(createMap(list.get(0)));
	  record.setId(database + "-" + id);
	  record.setDatabase(database);
	}
	if (list.size() > 1)
	  throw new RuntimeException("Too many results for id (" + id + ") lookup.");
	return null;
      } else
	throw new RuntimeException("Status id (" + id + ") lookup. Status: " + response.getStatus());
    } catch (SolrServerException e) {
      e.printStackTrace();
      throw new RuntimeException("Solr Server Exception on lookup: " + e.getMessage(), e);
    }
  }

  @Override
  synchronized public void delete(String ids) {
    try {
      UpdateResponse updateResponse = server.deleteById(ids);
      if (updateResponse.getStatus() != 0)
	logger.error("Delete record (" + ids + ") failed. Status: " + updateResponse.getStatus());
      else
	storageStatus.incrementAdd(1);
    } catch (SolrServerException e) {
      logger.error("SolrServer Exception  on delete: " + e.getMessage());
      e.printStackTrace();
    } catch (IOException e) {
      logger.error("IO Exception on delete: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @Override
  public void databaseEnd() {
    logger.info("Database " + database + " ended.");
  }

  public String getIdField() {
    return idField;
  }

  public void setIdField(String idField) {
    this.idField = idField;
  }

  public String getDatabaseField() {
    return databaseField;
  }

  public void setDatabaseField(String databaseField) {
    this.databaseField = databaseField;
  }

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  @Override
  public void setLogger(StorageJobLogger logger) {
    this.logger = logger;
  }

  @Override
  public StorageStatus getStatus()  {
    return storageStatus;
  }

  @Override
  public DatabaseContenthandler getContentHandler() {
    return new Pz2SolrRecordContentHandler(this, database);
  }

  public boolean isWaitSearcher() {
    return waitSearcher;
  }

  public void setWaitSearcher(boolean waitSearcher) {
    this.waitSearcher = waitSearcher;
  }

}
