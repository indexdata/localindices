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

public class Pz2SolrRecordContentHandler implements ContentHandler {
	
	RecordStorage store; 
	private RecordImpl record = null;
	private Map<String, Collection<Serializable>> keyValues = null;
	private StringBuffer currentText = null;
	private boolean inHeader = false;
	private boolean inMetadata = false;
	private Stack<StringBuffer> textBuffers = new Stack<StringBuffer>();
	private String databaseId;
	private String type;

	public Pz2SolrRecordContentHandler(RecordStorage storage, String database) {
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
		if (record == null && qName.equals("pz:record")) {
			inMetadata = true; 
			keyValues = new HashMap<String, Collection<Serializable>>();
			record = new RecordImpl(keyValues);
			record.setDatabase(databaseId);
		}
		if (record == null && localName.equals("delete")) {
			inHeader = true; 
			record = new RecordImpl();
			record.setDatabase(databaseId);
			record.setId(getAttributeValue(atts, "id"));
		}
		if (inMetadata && qName.equals("pz:metadata")) {
				type = getAttributeValue(atts, "type");
		}

	}
private String getAttributeValue(Attributes atts, String name) 
{
	for (int index = 0; index < atts.getLength(); index++)
		if (atts.getLocalName(index).equals(name)) 
			return atts.getValue(index);
		return null;
	}

@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (record != null) {
			if (inHeader && localName.equals("identifier")) {
				record.setId(currentText.toString());
			}
			if (localName.equals("header")) 
				inHeader = false; 
			if (localName.equals("record")) 
				inMetadata = false; 
			if (inMetadata) {
				Collection<Serializable> values = keyValues.get(type);
				
				if (values == null) {
					values = new LinkedList<Serializable>();
					keyValues.put(type, values);
				}
				values.add(currentText);
				if ("id".equals(type))
					record.setId(currentText.toString());
			}
		}
		/* This should work both with and without OAI-PMH headers */
		if (record != null && localName.equals("delete")) {
			store.delete(record.getId());
		}
		if (record != null && qName.equals("pz:record")) {
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
