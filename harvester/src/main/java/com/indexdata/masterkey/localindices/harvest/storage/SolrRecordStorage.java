package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TimeZone;

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
import org.apache.solr.client.solrj.SolrServer;

public class SolrRecordStorage extends SolrStorage implements RecordStorage {
  Collection<String> deleteIds = new LinkedList<String>();
  private String idField = "id";
  protected String databaseField = "database";
  private String database;
  @SuppressWarnings("unused")
  private Map<String, String> databaseProperties;
  private Date transactionId;
  private boolean delayedPurge = true;
  private boolean waitSearcher = false;
  private boolean isPurged;
  private String transactionIdField = "transactionId";
  private String harvestDateField = "harvest-timestamp";
  private String harvestDateStringField = "harvest-date";
  private String harvestDate; 
  private String harvestDateShort; 
  private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
  private SimpleDateFormat formatterShort = new SimpleDateFormat("yyyy-MM-dd");

  @SuppressWarnings("unused")
  private SolrServerFactory factory;
  
  public SolrRecordStorage() {
  }
  
  public SolrRecordStorage(Harvestable harvestable) {
    super(harvestable);
  }

  public SolrRecordStorage(String url, Harvestable harvestable) {
    super(url, harvestable);
  }

  public SolrRecordStorage(SolrServer server, Harvestable harvestable) {
    super(server, harvestable);
  }

  @Override
  synchronized public void begin() throws IOException {
    logger.info("Storage transaction begins...");
    super.begin();
    transactionId = new Date();
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    harvestDate =  formatter.format(transactionId);
    harvestDateShort =  formatterShort.format(transactionId);
    database = harvestable.getId().toString();
    storageStatus.setTransactionState(TransactionState.InTransaction);
  }

  @Override
  synchronized public void commit() throws IOException {
    logger.info("Storage transaction commits...");
    try {
      if (delayedPurge && isPurged) {
	purgeByTransactionId(false);
      }
      logger.info("Committing " + storageStatus.getOutstandingAdds() + " added and " + storageStatus.getOutstandingDeletes() + " deleted records to database " + database);
      // Testing waitFlush=true, waitSearcher=false. Not good for indexes with searchers, but better for crawlers. 
      UpdateResponse response = server.commit(true, waitSearcher);
      if (response.getStatus() != 0)
	logger.error("Error while COMMITING records.");
      else {	
	storageStatus.setTransactionState(TransactionState.Committed);
      }
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
      logger.error("Failed to purge by id. Exception: " + e.getMessage());
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
    if (idField != null) {
      // TODO prioritize Record.getId() vs "id" after transformation
      if (record.getId() != null)
	document.setField(idField, database + "-" + record.getId());
      else if (record.getValues().get(idField) != null) {
	document.setField(idField, database + record.getValues().get(idField).toString());
      }
      else 
	logger.error("Failed to get Record Id for record: " + record);
    }
    if (databaseField != null)
      document.setField(databaseField, database);
    
    if (transactionId  != null) {
      document.setField(transactionIdField, transactionId.getTime());
      document.setField(harvestDateField, harvestDate);
      document.setField(harvestDateStringField, harvestDateShort);
    }
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
	((SolrStorageStatus) storageStatus).incrementAdd(1);
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
	  throw new StorageException("Too many results for id (" + id + ") lookup.");
	return null;
      } else
	throw new StorageException("Status id (" + id + ") lookup. Status: " + response.getStatus());
    } catch (SolrServerException e) {
      e.printStackTrace();
      throw new StorageException("Solr Server Exception on lookup: " + e.getMessage(), e);
    }
  }

  @Override
  synchronized public void delete(String ids) {
    try {
      UpdateResponse updateResponse = server.deleteById(ids);
      if (updateResponse.getStatus() != 0)
	logger.error("Delete record (" + ids + ") failed. Status: " + updateResponse.getStatus());
      else
	((SolrStorageStatus) storageStatus).incrementDelete(1);
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
  public DatabaseContenthandler getContentHandler() {
    return new Pz2SolrRecordContentHandler(this, database);
  }

  public boolean isWaitSearcher() {
    return waitSearcher;
  }

  public void setWaitSearcher(boolean waitSearcher) {
    this.waitSearcher = waitSearcher;
  }

  @Override
  public void shutdown() {
    storageStatus =  new SimpleStorageStatus(storageStatus);
    logger.info("SolrRecordStorage shutdown");
    server.shutdown();
  }

}
