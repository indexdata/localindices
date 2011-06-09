/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

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

import ORG.oclc.oai.harvester2.verb.HarvesterVerb;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.entity.SolrXmlBulkResource;
import com.indexdata.masterkey.localindices.entity.WebCrawlResource;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;

/**
 * Returns an instance of a HarvestStorage object.
 * @author jakub
 */
public class HarvestStorageFactory {

    public HarvestStorageFactory() {
    }

	static String[] marc21 = { "oai2marc.xsl", "marc21.xsl", "pz2-solr.xsl"};
	static String[] dc     = { "oai_dc.xsl", 				 "pz2-solr.xsl"};

	public static HarvestStorage getStorage(Harvestable harvestable) {
		HarvestStorage harvestStorage = null;
		if (harvestable.getStorage() instanceof com.indexdata.masterkey.localindices.entity.SolrStorage) {
			com.indexdata.masterkey.localindices.entity.SolrStorage entity = (com.indexdata.masterkey.localindices.entity.SolrStorage) harvestable.getStorage();
				return new BulkSolrRecordStorage(entity.getUrl(), harvestable);
		}
		
		return harvestStorage; 
	}

	
	public static HarvestStorage getStorage(String storageDir, Harvestable harvestable) {
        HarvestStorage st = null;
        
        SolrStorage storage = new SolrStorage(storageDir, harvestable);
        SolrRecordStorage recordStorage = new BulkSolrRecordStorage(storageDir, harvestable);
        boolean useRecordStorage = true;
        if (harvestable instanceof OaiPmhResource) {
            if (useRecordStorage)
            	return recordStorage;
            else {
            	String[] chain = null;
                if (((OaiPmhResource) harvestable).getMetadataPrefix().equalsIgnoreCase("marc21"))
                	chain = marc21;
                else       
                	chain = dc;
	            try {
		        	XMLReader xmlFilter = createXMLFilter(chain);
					st = new TransformationChainStorageProxy(storage, xmlFilter);
				} catch (TransformerConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new RuntimeException("Configuration error", e);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new RuntimeException("IOException error while creating Storage", e);
				}
            }
        } else if (harvestable instanceof XmlBulkResource) {
            st = new ZebraFileStorage(storageDir, harvestable, "");
            st.setOverwriteMode(true);
        } else if (harvestable instanceof SolrXmlBulkResource) {
            st = new SolrStorage(storageDir, harvestable);
            st.setOverwriteMode(true);
        } else if (harvestable instanceof WebCrawlResource) {
            st = new ZebraFileStorage(storageDir, harvestable, "pz-pz.xml");
            st.setOverwriteMode(true);
        }
        return st;
    }

	public static XMLFilter createXMLFilter(String[] stylesheets) throws TransformerConfigurationException {
		// Set up to read the input file
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
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
		// Create the filters
		// --SAXTransformerFactory is an interface
		// --TransformerFactory is a concrete class
		// --TransformerFactory actually returns a SAXTransformerFactory instance
		// --We didn't care about that before, because we didn't use the
		// --SAXTransformerFactory extensions. But now we do, so we cast the result.
		SAXTransformerFactory stf = (SAXTransformerFactory) TransformerFactory.newInstance();
		XMLFilter filter = null;
		XMLReader parent = reader; 
		int index = 0;
		try {
			while (index < stylesheets.length ) {
				filter = stf.newXMLFilter(new StreamSource(new FileInputStream(stylesheets[index]) ));
				filter.setParent(parent);
				parent = filter;
				index++;
			}
		} catch (FileNotFoundException fnfe) {
			File file = new File(".");
			throw new TransformerConfigurationException("Stylesheet not found in " + file.getAbsoluteFile() + ": " + stylesheets[index] + ". " + fnfe.getMessage(), fnfe);
		}
		return filter;
}
}
