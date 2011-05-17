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

public class SolrRecordStorage extends SolrStorage implements RecordStorage 
{
	Collection<String> deleteIds = new LinkedList<String>();
	private String idField;
	private String databaseField;
	private String database;
	@SuppressWarnings("unused")
	private Map<String, String> databaseProperties;
	protected int added;
	protected int deleted;
	
	public SolrRecordStorage(String url_string, Harvestable harvestable) {
		super(url_string, harvestable);
	}

	@Override 
	public void begin() throws IOException {
		super.begin();
		added = 0;
		deleted = 0;
	}
	
	@Override
	public void commit() throws IOException {
		try {

			logger.info("Committing added " + added + " and deleted " + deleted + " records.");
			UpdateResponse response = server.commit();
			if (response.getStatus() != 0)
				logger.error("Error while COMMITING records.");
		} catch (SolrServerException e) {
			logger.error("Commit failed when adding " + added + " and deleting " + deleted + ".");
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
				// TODO Remove here, but enable somehow.
				server.deleteByQuery("*:*");
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
		if (properties.get(databaseField) != null)
			database = properties.get(databaseField); 
		if (database == null)
			logger.warn("No database field (" + databaseField + ") found in properties"); 
	}

	// @Deprecated createDocument Use Record method
	protected SolrInputDocument createDocument(Map<String, Collection<Serializable>> keyValues) {
		boolean foundIdField = false;
		SolrInputDocument document = new SolrInputDocument();
		for (String key : keyValues.keySet()) {
			for (Serializable value : keyValues.get(key)) {
				if (key.equals(idField)) 
					foundIdField = true;
				document.addField(key, value);
			}
		}
		if (foundIdField) 
			throw new RuntimeException("No identifier found.");
		return document;
	}

	protected SolrInputDocument createDocument(Record record) {
		SolrInputDocument document = createDocument(record.getValues());
		if (idField != null)
			document.setField(idField, record.getId());
		else 
			document.setField("id", record.getId());
		if (database != null)
			document.setField("database", record.getDatabase());  
		return document;
	}

	public void add(SolrInputDocument document) {
		try {
			UpdateResponse updateResponse = server.add(document);
			if (updateResponse.getStatus() != 0 ) 
				logger.error("Add record (" + document + ") failed. Status: " + updateResponse.getStatus());
			else
				added++;
		} catch (SolrServerException e) {
			logger.error("SolrServer Exception on add: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			logger.error("IO Exception on add: " + e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void add(Map<String, Collection<Serializable>> keyValues) {
		add(createDocument(keyValues));
	}
	
	@Override
	public void add(Record record) {
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
	public void delete(String ids) {
		try {
			UpdateResponse updateResponse = server.deleteById(ids);
			if (updateResponse.getStatus() != 0 ) 
				logger.error("Delete record (" + ids + ") failed. Status: " + updateResponse.getStatus());
			else
				deleted++;
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
	
}
