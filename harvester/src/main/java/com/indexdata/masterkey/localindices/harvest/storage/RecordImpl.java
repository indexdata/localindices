package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public class RecordImpl implements Record {

	Map<String, Collection<Serializable>> valueMap;
	String id; 
	String database; 
	public RecordImpl(Map<String, Collection<Serializable>> values) {
		valueMap = values;
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getDatabase() {
		return database;
	}

	@Override
	public Map<String, Collection<Serializable>> getValues() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

}
