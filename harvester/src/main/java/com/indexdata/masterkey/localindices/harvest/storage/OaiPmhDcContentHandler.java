package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class OaiPmhDcContentHandler implements ContentHandler {
	
	RecordStorage store; 
	private RecordImpl record = null;
	private Map<String, Collection<Serializable>> keyValues = null;
	private Boolean isDeleted = null;
	private StringBuffer currentText = null;
	private boolean inHeader = false;
	private boolean inMetadata = false;
	private Stack<StringBuffer> textBuffers = new Stack<StringBuffer>();
	private String databaseId; 
	
	public OaiPmhDcContentHandler(RecordStorage storage, String database) {
		store = storage;
		databaseId = database;
	}
	
	@Override
	public void setDocumentLocator(Locator locator) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startDocument() throws SAXException {
		// TODO Auto-generated method stub
		currentText = new StringBuffer();
	}

	@Override
	public void endDocument() throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		textBuffers.add(currentText);
		currentText = new StringBuffer();
		if (localName.equals("record")) {
			keyValues = new HashMap<String, Collection<Serializable>>();
			record = new RecordImpl(keyValues);
			record.setDatabase(databaseId);
		}
		if (record != null && localName.equals("header")) {
			inHeader = true; 
			isDeleted = getDeleteStatus(atts);
		}
		if (record != null && localName.equals("dc"))
			inMetadata = true; 
		
	}

	private Boolean getDeleteStatus(Attributes atts) {
		for (int index = 0; index < atts.getLength(); index++)
			if (atts.getLocalName(index).equals("status") && atts.getValue(index).equals("deleted"))
				return true;
		return false;
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		// TODO Auto-generated method stub
		if (record != null) {
			if (inHeader && localName.equals("identifier")) {
				record.setId(currentText.toString());
			}
			if (localName.equals("header")) 
				inHeader = false; 
			if (localName.equals("dc")) 
				inMetadata = false; 
			if (inMetadata) {	
				Collection<Serializable> values = keyValues.get(localName);
				if (values == null) {
					values = new LinkedList<Serializable>();
					keyValues.put(localName, values);
				}
				values.add(currentText);
			}
		}
		if (localName.equals("record")) {
			if (isDeleted)
				store.delete(record.getId());
			else 
				store.add(record);
			record = null; 
		}
		currentText = textBuffers.pop();
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		currentText.append(new String(ch, start, length));
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processingInstruction(String target, String data)
			throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		// TODO Auto-generated method stub
		
	}

}
