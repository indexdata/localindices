package com.indexdata.masterkey.localindices.harvest.storage;

import java.util.Map;

import org.w3c.dom.Node;

public interface XmlHarvestStorage {

	void begin();
	void commit();
	void rollback();

	void purge();

	void beginDocument(String name, Map<String, String> attributes);
	void endDocument();

	void beginElement(String name, Map<String, String> attributes);
	void endElement();
	
	void store(Node node);
	
	

}
