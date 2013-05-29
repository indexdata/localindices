/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.entity.FileStorageEntity;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.xml.factory.XmlFactory;

/**
 * Returns an instance of a HarvestStorage object.
 * 
 * @author jakub
 * @author Dennis
 */
public class HarvestStorageFactory {

  /**
   * Creates a HarvestStorage based on the Database Entity
   * 
   * @param harvestable
   * @return
   */
  public static HarvestStorage getStorage(Harvestable harvestable) {
    HarvestStorage harvestStorage = null;
    Storage entity = (Storage) harvestable.getStorage();
    /* TODO Extend to create other types */
    if (entity instanceof com.indexdata.masterkey.localindices.entity.SolrStorageEntity) {
      SolrStorage storage = new BulkSolrRecordStorage(entity.getUrl(), harvestable);
      storage.setStorageId("" + entity.getId());
      return storage;
    }
    else if (entity instanceof FileStorageEntity) {
      FileStorage storage = new FileStorage();
      storage.setHarvestable(harvestable);
      return storage;
    }
    else {
      if (entity.getIdAsString() != null) {
	try {
	  Class<?> storage = Class.forName(entity.getIdAsString());
	  Object object = storage.newInstance();
	  if (object instanceof HarvestStorage) {
	    return (HarvestStorage) object;
	  }
	} catch (ClassNotFoundException e) {
	  e.printStackTrace();
	} catch (InstantiationException e) {
	  // TODO Auto-generated catch block
	  e.printStackTrace();
	} catch (IllegalAccessException e) {
	  // TODO Auto-generated catch block
	  e.printStackTrace();
	}	
      }
    }
    return harvestStorage;
  }
   static SAXParserFactory spf = XmlFactory.newSAXParserFactoryInstance();

  /**
   * Creates a XMLFilter from an array of strings
   * 
   * @param stylesheets
   * @return
   * @throws TransformerConfigurationException
   * @throws UnsupportedEncodingException
   * 
   *           TODO move out of this.
   */
  public static XMLFilter createXMLFilter(String[] stylesheets)
      throws TransformerConfigurationException, UnsupportedEncodingException {
    XMLReader reader;
    SAXParser parser;
    try {
      parser = spf.newSAXParser();
      reader = parser.getXMLReader();
    } catch (ParserConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new TransformerConfigurationException("Parser Configuration Error", e);
    } catch (SAXException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new TransformerConfigurationException("SAX Exception", e);
    }

    SAXTransformerFactory stf = (SAXTransformerFactory) XmlFactory.newTransformerInstance();
    XMLFilter filter = null;
    XMLReader parent = reader;
    int index = 0;
    while (index < stylesheets.length) {
      filter = stf.newXMLFilter(new StreamSource(new ByteArrayInputStream(stylesheets[index]
	  .getBytes("UTF-8"))));
      filter.setParent(parent);
      parent = filter;
      index++;
    }
    return filter;
  }
}
