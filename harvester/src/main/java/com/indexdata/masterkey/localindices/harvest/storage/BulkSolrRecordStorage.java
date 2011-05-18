package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import com.indexdata.masterkey.localindices.entity.Harvestable;

public class BulkSolrRecordStorage extends SolrRecordStorage {

	Collection<SolrInputDocument> docs = new LinkedList<SolrInputDocument>();
	List<String> deleteIds = new LinkedList<String>();
	Integer limit = 1000;
	public BulkSolrRecordStorage(String url_string, Harvestable harvestable) {
		super(url_string, harvestable);
		// TODO Auto-generated constructor stub
	}
	
	public void add(Record record) {
		if (deleteIds.size() > 0)
			deleteRecords();
		docs.add(createDocument(record));
		if (limit != null && docs.size() >= limit)
			addRecords();
	}

	public void delete(String id) {
		if (docs.size() > 0)
			addRecords();
		deleteIds.add(id);
		if (limit != null && deleteIds.size() >= limit)
			deleteRecords();
	}

	private void addRecords() {
		try {
			UpdateResponse response = null;
			int no_docs = docs.size();
			logger.info("Adding " + no_docs + " records.");
			try {
				 response = server.add(docs);
			} catch (SolrServerException ste) {
				logger.error("Exception while adding documents. Outstanding adds: " + added + ". Deletes: " + deleted);
				throw ste;
			}
			if (response.getStatus() != 0)
				logger.error("Error adding documents");
			else
				added += no_docs;
			docs = new LinkedList<SolrInputDocument>();
		} catch (SolrServerException e) {
			e.printStackTrace();
			throw new RuntimeException("Solr Server Exception while adding records", e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("IO Exception while adding records", e);
		}
	}

	private void deleteRecords() {
		try {
			UpdateResponse response = null; 
			int no_docs = deleteIds.size();
			logger.info("Deleting " + no_docs + " records.");
			try {
				response = server.deleteById(deleteIds);
			} catch (SolrServerException ste) {
				logger.error("Exception while deleting documents after adding " + added + "and deleting " + deleted);
				throw ste;
			}
			if (response.getStatus() != 0)
				logger.error("Error deleting documents");
			else
				deleted += no_docs;			
			deleteIds = new LinkedList<String>();
		} catch (SolrServerException e) {
			e.printStackTrace();
			throw new RuntimeException("Solr Server Exception while deleting records", e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("IO Exception while delete records", e);
		}
	}
	
	public void commit() throws IOException {
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
