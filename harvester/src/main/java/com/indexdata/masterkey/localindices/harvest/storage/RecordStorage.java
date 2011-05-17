package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public interface RecordStorage extends HarvestStorage {
	
	void databaseStart(Map<String ,String> properties);
	void databaseEnd();
	void add(Map<String, Collection<Serializable>> keyValues);
	void add(Record record);
	Record get(String id);
	// void remove(Collection<String> ids);
	void delete(String id);

}
