package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

import com.indexdata.masterkey.localindices.entity.Harvestable;

public class BulkSolrRecordStore extends SolrRecordStore {

	Collection<SolrInputDocument> docs = new LinkedList<SolrInputDocument>();
	List<String> deleteIds = new LinkedList<String>();
	
	public BulkSolrRecordStore(String url_string, Harvestable harvestable) {
		super(url_string, harvestable);
		// TODO Auto-generated constructor stub
	}

	
	public void add(Record record) {
		if (deleteIds.size() > 0)
			deleteRecords();
		docs.add(createDocument(record.getValues()));
	}

	public void delete(String id) {
		if (docs.size() > 0)
			addRecords();
		deleteIds.add(id);
	}


	private void addRecords() {
		try {
			server.add(docs);
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
			server.deleteById(deleteIds);
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
