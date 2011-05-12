package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
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

public class SolrRecordStore extends SolrStorage implements RecordStorage 
{
	Collection<String> deleteIds = new LinkedList<String>();
	private String idField;
	private String databaseField;
	private String database;
	private Map<String, String> databaseProperties;
	
	public SolrRecordStore(String url_string, Harvestable harvestable) {
		super(url_string, harvestable);
	}

	@Override
	public void commit() throws IOException {
		try {
			server.commit();
		} catch (SolrServerException e) {
			e.printStackTrace();
			throw new RuntimeException("Commit failed: " + e.getMessage(), e);
		}
	}

	@Override
	public void rollback() throws IOException {
		try {
			server.rollback();
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Rollback failed: " + e.getMessage(), e);
		}
	}

	@Override
	public void purge() throws IOException {
		try {
			if (database != null) 
				server.deleteByQuery("database:" + database);
			else {
				server.deleteByQuery("database: *");
			}
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Error purging database (" + database + ")");
		}
	}

	@Override
	public OutputStream getOutputStream() {
		throw new RuntimeException("Not supporting OutputStream interface.");
	}

	@Override
	public void databaseStart(Map<String, String> properties) {
		databaseProperties = properties; 
		database = properties.get(databaseField);
		if (database == null)
			logger.warn("No database field (" + databaseField + ") found in properties"); 
	}

	protected SolrInputDocument createDocument(Map<String, Collection<Serializable>> keyValues) {
		SolrInputDocument document = new SolrInputDocument(); 
		for (String key : keyValues.keySet()) {
			for (Serializable value : keyValues.get(key)) {
				document.addField(key, value);
			}
		}
		return document;
	}
	
	@Override
	public void add(Map<String, Collection<Serializable>> keyValues) {
		try {
			SolrInputDocument doc = createDocument(keyValues);
			UpdateResponse updateResponse = server.add(doc);
			if (updateResponse.getStatus() != 200 ) 
				logger.error("Add record (" + doc + ") failed. Status: " + updateResponse.getStatus());
		} catch (SolrServerException e) {
			logger.error("SolrServer Exception on add: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			logger.error("IO Exception on add: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	@Override
	public void add(Record record) {
		add(record.getValues());
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
		Map<String, Collection<Serializable>> map  = new HashMap<String, Collection<Serializable>>();
		for (String field : solrDocument.getFieldNames()) {
			map.put(field, getValueAsCollection(solrDocument.getFieldValue(field)));
		}
		return map;
	}
	@Override
	public Record get(String id) {
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set(idField, id);
		if (database != null) 
			params.set(databaseField, database);
		
		try {
			QueryResponse response = server.query(params);
			if (response.getStatus() == 200) {
				SolrDocumentList list = response.getResults();
				if (list.size() == 1) {
					RecordImpl record = new RecordImpl(createMap(list.get(0)));
					record.setId(id);
					record.setDatabase(database);
				}
				if (list.size() > 1)
					throw new RuntimeException("Too many results for id (" + id + ") lookup.");
				return null;
			}
			else 
				throw new RuntimeException("Status id (" + id + ") lookup. Status: " + response.getStatus());
		} catch (SolrServerException e) {
			e.printStackTrace();
			throw new RuntimeException("Solr Server Exception on lookup: " + e.getMessage(), e);
		}
	}

	@Override
	public void remove(String ids) {
		try {
			UpdateResponse updateResponse = server.deleteById(ids);
			if (updateResponse.getStatus() != 200 ) 
				logger.error("Delete record (" + ids + ") failed. Status: " + updateResponse.getStatus());
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
		logger.info("Database " +  databaseProperties.get(databaseField) + " ended.");
	}
	
}
