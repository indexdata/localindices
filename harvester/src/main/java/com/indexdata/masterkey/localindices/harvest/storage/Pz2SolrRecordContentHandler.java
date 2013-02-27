package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Locator2;

public class Pz2SolrRecordContentHandler implements DatabaseContenthandler {

  RecordStorage store;
  private RecordImpl record = null;
  private Map<String,String> prefixes = new HashMap<String, String>();
  private Map<String, Collection<Serializable>> keyValues = null;
  private StringBuffer currentText = null;
  private boolean inHeader = false;
  private boolean inMetadata = false;
  private Stack<StringBuffer> textBuffers = new Stack<StringBuffer>();
  private String databaseId;
  private String type;
  private Logger logger = Logger.getLogger(this.getClass());
  @SuppressWarnings("unused")
  private String encoding = "UTF-8";
  private Locator2 locator;
  private String mergekey = "author;title";
  
  public Pz2SolrRecordContentHandler(RecordStorage storage, String database) {
    store = storage;
    databaseId = database;
  }

  public void setDatabaseIdentifier(String id) {
    databaseId = id;
  }
  
    @Override
    public void setDocumentLocator(Locator locator) {
        if (locator instanceof Locator2) {
            this.locator = (Locator2) locator;
        }
    }
    
    @Override
    public void startDocument() throws SAXException {
        if (locator != null) {
            this.encoding = locator.getEncoding();
        }
        currentText = new StringBuffer(/* "<?xml version=\"1.0\" encoding=\"" + encoding +"\" ?>" */);
  }

  @Override
  public void endDocument() throws SAXException {
    // TODO Auto-generated method stub
  }

  @Override
  public void startPrefixMapping(String prefix, String uri) throws SAXException {
    //logger.trace("start prefixMapping: " + prefix + " " + uri);
    prefixes.put(prefix, uri);
  }

  @Override
  public void endPrefixMapping(String prefix) throws SAXException {
    //logger.trace("end prefixMapping: " + prefix);
    prefixes.remove(prefix);
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts)
      throws SAXException {
    textBuffers.add(currentText);
    currentText = new StringBuffer();
    if (record == null && (qName.equals("pz:record") || localName.equals("record"))) {
      inMetadata = true;
      keyValues = new HashMap<String, Collection<Serializable>>();
      record = new RecordImpl(keyValues);
      record.setDatabase(databaseId);
    }
    if (record == null && localName.equals("delete")) {
      inHeader = true;
      record = new RecordImpl();
      record.setDatabase(databaseId);
      record.setId(databaseId + "-" + getAttributeValue(atts, "id"));
    }
    if (inMetadata && (qName.equals("pz:metadata") || localName.equals("metadata"))) {
      type = getAttributeValue(atts, "type");
    }

  }

  private String getAttributeValue(Attributes atts, String name) {
    for (int index = 0; index < atts.getLength(); index++)
      if (atts.getLocalName(index).equals(name))
	return atts.getValue(index);
    return null;
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if (record != null) {
      if (inHeader && localName.equals("identifier")) {
	record.setId(databaseId + "-" + currentText.toString());
      }
      if (localName.equals("header"))
	inHeader = false;
      if (localName.equals("record")) {
	if (record.getId() == null) {
	  // TODO generate a "merge record id"
	  // record.setId(genenerateId(record));
	}
	inMetadata = false;
      }
      if (inMetadata && type != null) {
	Collection<Serializable> values = keyValues.get(type);
	
	if (values == null) {
	  values = new LinkedList<Serializable>();
	  keyValues.put(type, values);
	}
	values.add(currentText);
	if ("id".equals(type))
	  record.setId(databaseId + "-" + currentText.toString());
      }
    }
    /* This should work both with and without OAI-PMH headers */
    if (record != null && localName.equals("delete")) {
      store.delete(record.getId());
    }
    if (record != null && (qName.equals("pz:record") || localName.equals("record"))) {
      store.add(record);
      record = null;
    }
    currentText = textBuffers.pop();
  }

  // TODO later
  @SuppressWarnings("unused")
  private String genenerateId(RecordImpl record) {
    String [] keys = mergekey.split(";");
    Map<String, Collection<Serializable>> values = record.getValues();
    StringBuffer buffer = new StringBuffer(""); 
    for (String key : keys) {
      Collection<Serializable> objectList = values.get(key);
      if (objectList != null) {
	Iterator<Serializable> iterator = objectList.iterator();
	while (iterator.hasNext()) {
	  Serializable obj = iterator.next();
	  if (obj != null)
	    buffer.append("author: " + obj.toString());
	}
      }
    }
    return null;
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    currentText.append(new String(ch, start, length));
  }

  @Override
  public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    // TODO Auto-generated method stub

  }

  @Override
  public void processingInstruction(String target, String data) throws SAXException {
    // TODO Auto-generated method stub

  }

  @Override
  public void skippedEntity(String name) throws SAXException {
    logger.trace("Skipped Entity: " + name);
  }

  @Override
  public void setDatebaseIdentifier(String id) {
    databaseId = id;
  }

  @Override
  public String getDatebaseIdentifier() {
    return databaseId;
  }
}
