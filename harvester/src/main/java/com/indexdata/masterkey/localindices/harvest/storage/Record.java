package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public interface Record {
	String getId();
	String getDatabase();
	Map<String, Collection<Serializable>> getValues();

}
