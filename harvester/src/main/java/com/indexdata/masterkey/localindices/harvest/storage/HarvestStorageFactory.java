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
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.entity.Harvestable;

/**
 * Returns an instance of a HarvestStorage object.
 * 
 * @author jakub
 * @author Dennis
 */
public class HarvestStorageFactory {

	/**
	 * Creates a HarvestStorage based on the Database Entity
	 * @param harvestable
	 * @return
	 */
	public static HarvestStorage getStorage(Harvestable harvestable) {
		HarvestStorage harvestStorage = null;
		/* TODO Extend to create other types */
		if (harvestable.getStorage() instanceof com.indexdata.masterkey.localindices.entity.SolrStorage) {
			com.indexdata.masterkey.localindices.entity.SolrStorage entity = (com.indexdata.masterkey.localindices.entity.SolrStorage) harvestable
					.getStorage();
			return new BulkSolrRecordStorage(entity.getUrl(), harvestable);
		}

		return harvestStorage;
	}

	static SAXParserFactory spf = SAXParserFactory.newInstance();

	/**
	 * Creates a XMLFilter from an array of strings 
	 * @param stylesheets
	 * @return
	 * @throws TransformerConfigurationException
	 * @throws UnsupportedEncodingException
	 * 
	 * TODO move out of this.
	 */
	public static XMLFilter createXMLFilter(String[] stylesheets)
			throws TransformerConfigurationException,
			UnsupportedEncodingException {
		// Set up to read the input file
		spf.setNamespaceAware(true);
		XMLReader reader;
		SAXParser parser;
		try {
			parser = spf.newSAXParser();
			reader = parser.getXMLReader();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new TransformerConfigurationException(
					"Parser Configuration Error", e);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new TransformerConfigurationException("SAX Exception", e);
		}
		// Create the filters
		// --SAXTransformerFactory is an interface
		// --TransformerFactory is a concrete class
		// --TransformerFactory actually returns a SAXTransformerFactory
		// instance
		// --We didn't care about that before, because we didn't use the
		// --SAXTransformerFactory extensions. But now we do, so we cast the
		// result.
		SAXTransformerFactory stf = (SAXTransformerFactory) TransformerFactory
				.newInstance();
		XMLFilter filter = null;
		XMLReader parent = reader;
		int index = 0;
		while (index < stylesheets.length) {
			filter = stf.newXMLFilter(new StreamSource(
					new ByteArrayInputStream(stylesheets[index]
							.getBytes("UTF-8"))));
			filter.setParent(parent);
			parent = filter;
			index++;
		}
		return filter;
	}
}
