package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public interface Record {
	String getId();
	String getDatabase();
	boolean isDeleted();
	Map<String, Collection<Serializable>> getValues();

}
