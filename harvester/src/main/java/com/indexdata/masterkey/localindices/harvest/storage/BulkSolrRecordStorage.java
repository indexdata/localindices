package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;

import com.indexdata.masterkey.localindices.entity.Harvestable;

public class BulkSolrRecordStorage extends SolrRecordStorage {

  Collection<SolrInputDocument> docs = new LinkedList<SolrInputDocument>();
  List<String> deleteIds = new LinkedList<String>();
  Integer limit = 1000; 
  public BulkSolrRecordStorage(String solrUrl, Harvestable harvestable) {
    super(solrUrl, harvestable);
  }

  synchronized public void add(Record record) {
    if (deleteIds.size() > 0)
      deleteRecords();
    if (record.getValues().get("id") != null || record.getId() != null) { 
      	SolrInputDocument document = createDocument(record);
      	if (document != null)
      	  docs.add(document);
      	else
      	  logger.warn( "Failed to convert Record to SolrDocument. Not adding: " + record);
    }
    else
      	logger.warn( "No Id on Record. Not adding: " + record);
    if (limit != null && docs.size() >= limit)
      addRecords();
  }

  synchronized public void delete(String id) {
    if (docs.size() > 0)
      addRecords();
    deleteIds.add(id);
    if (limit != null && deleteIds.size() >= limit)
      deleteRecords();
  }

  private void addRecords() {
    int no_docs = docs.size();
    try {
      UpdateResponse response = null;
      logger.info("Adding " + no_docs + " records.");
      response = server.add(docs);
      if (response.getStatus() != 0) {
	logger.error("Error adding documents. HTTP Status code: " + response.getStatus());
	throw new RuntimeException("Error adding documents. HTTP error: " + response.getStatus());
      }
      else {
	 storageStatus.incrementAdd(no_docs);
	 docs = new LinkedList<SolrInputDocument>();
      }
    } catch (SolrException ste) {
      logger.error("Solr Exception while adding documents. Outstanding adds: " + no_docs
	 + ". Deletes: " + deleteIds.size() , ste);
      docs = new LinkedList<SolrInputDocument>();
      throw new RuntimeException("Solr Exception while adding records: " + ste.getMessage(), ste);
    } catch (SolrServerException ste) {
      logger.error("Solr Server Exception while adding documents. Outstanding adds: " + no_docs + ". Deletes: " + deleteIds.size(), ste);
      // TODO add docs to error queue
      docs = new LinkedList<SolrInputDocument>();
      throw new RuntimeException("Solr Server Exception while adding records: " + ste.getMessage(), ste);
    } catch (IOException e) {
      e.printStackTrace();
      // TODO Add to failed records queue
      deleteIds = new LinkedList<String>();
      throw new RuntimeException("IO Exception while adding records: " + e.getMessage(), e);
    }
  }

  private void deleteRecords() {
    try {
      UpdateResponse response = null;
      int no_docs = deleteIds.size();
      logger.info("Deleting " + no_docs + " records.");
      response = server.deleteById(deleteIds);
      if (response.getStatus() != 0)
	logger.error("Error deleting documents: " + response.getResponse());
      else
	storageStatus.incrementDelete(no_docs);
      deleteIds = new LinkedList<String>();
    } catch (SolrServerException e) {
      // TODO Add to failed records queue
      deleteIds = new LinkedList<String>();
      throw new RuntimeException("Solr Server Exception while deleting records", e);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("IO Exception while delete records", e);
    }
  }

  synchronized public void commit() throws IOException {
    // Flush outstanding operations. Should either be add or delete, not both
    if (docs.size() > 0) {
      addRecords();
    }
    if (deleteIds.size() > 0) {
      deleteRecords();
    }
    super.commit();
  }
}
