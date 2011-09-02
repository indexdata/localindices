/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.job;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.crawl.HTMLPage;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.harvest.storage.Pz2SolrRecordContentHandler;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.TransformationChainRecordStorageProxy;

/**
 * This class handles bulk HTTP download of a single file.
 * @author Dennis
 * 
 */
public class BulkRecordHarvestJob extends AbstractRecordHarvestJob {

    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");

    private String error;
    @SuppressWarnings("unused")
	private List<URL> urls = new ArrayList<URL>();
    private XmlBulkResource resource;
    private Proxy proxy;
    private boolean die = false;

    public BulkRecordHarvestJob(XmlBulkResource resource, Proxy proxy) {
    	this.proxy = proxy;
        this.resource = resource;
        this.resource.setMessage(null);
        setStatus(HarvestStatus.valueOf(resource.getCurrentStatus()));
    }

    private RecordStorage setupTransformation(RecordStorage storage) {
    	if (resource.getTransformation() != null && resource.getTransformation().getSteps().size() > 0) {
    		List<TransformationStep> steps = resource.getTransformation().getSteps();
    		String[] stylesheets = new String[steps.size()]; 
    		int index = 0;
    		for (TransformationStep step : steps) {
    			if (step.getScript() != null) {
    				stylesheets[index] = step.getScript();
    				index++;
    			}
    		}
        	XMLReader xmlReader;
			try {
				xmlReader = createTransformChain(stylesheets);
	    		return new TransformationChainRecordStorageProxy(storage, xmlReader,new Pz2SolrRecordContentHandler(storage, resource.getId().toString()));
	    		
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e.getMessage());
			}
    	}
    	logger.warn("No Transformation Proxy configured.");
    	return storage;
    }

	private synchronized boolean isKillSendt() {
        if (die) {
            logger.log(Level.WARN, "Bulk harvest received kill signal.");
        }
        return die;
    }

    public String getMessage() {
        return error;
    }

    public void run() {
        try {
            setStatus(HarvestStatus.RUNNING);
            //db drop mode on
            getStorage().setOverwriteMode(true);
            downloadList(resource.getUrl().split(" "));
            setStatus(HarvestStatus.FINISHED);
        } catch (Exception e) {
            setStatus(HarvestStatus.ERROR);
            error = e.getMessage();
            resource.setMessage(e.getMessage());
            logger.log(Level.ERROR, "Download failed.", e);
        }
    }

    private void downloadList(String[] urls) throws Exception {
        for (String url : urls) {
            download(new URL(url));
        }
    }

    private void download(URL url) throws Exception {
        logger.log(Level.INFO, "Starting download - " + url.toString());
        try {
            HttpURLConnection conn = null;
            if (proxy != null)
                conn = (HttpURLConnection) url.openConnection(proxy);
            else
                conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            int contentLenght = conn.getContentLength();
            String contentType = conn.getContentType();
            if (responseCode == 200) {
                //jump page
                if (contentType.startsWith("text/html")) {
                    logger.log(Level.INFO, "Jump page found at " + url.toString());
                    HTMLPage jp = new HTMLPage(conn.getInputStream(), url);
                    if (jp.getLinks().isEmpty()) {
                        throw new Exception("No links found on the jump page");
                    }
                    int proper = 0;
                    int dead = 0;
                    int recursive = 0;
                    for (URL link : jp.getLinks()) {
                        if (proxy != null)
                            conn = (HttpURLConnection) link.openConnection(proxy);
                        else
                            conn = (HttpURLConnection) link.openConnection();
                        conn.setRequestMethod("GET");
                        responseCode = conn.getResponseCode();
                        contentLenght = conn.getContentLength();
                        contentType = conn.getContentType();
                        if (responseCode == 200) {
                            // watch for non-marc links
                            if (contentType.startsWith("text/html")) {
                                logger.log(Level.WARN, "Possible sub-link ignored at " + link.toString());
                                recursive++;
                                continue;
                            // possibly a marc file
                            } else {
                                logger.log(Level.INFO, "Found file at " + link.toString());
                                store(conn.getInputStream(), contentLenght);
                                getStorage().setOverwriteMode(false);
                                proper++;
                            }
                        } else {
                            logger.log(Level.WARN, "Dead link (" + responseCode + " at " + link.toString());
                            dead++;
                            continue;
                        }
                    }
                    if (proper == 0) {
                      logger.log(Level.ERROR, "No proper links found at " + url.toString() +
                                ", trash links: " + recursive +
                                ", dead links: " + dead);
                       throw new Exception("No MARC files found at "+url.toString());
                    }
                } else {
                	//setupTransformation()
                    store(conn.getInputStream(), contentLenght);
                    getStorage().setOverwriteMode(false);
                    return;
                }
            } else {
                throw new Exception("Http connection failed. (" + responseCode + ")");
            }
            logger.log(Level.INFO, "Finished - " + url.toString());
        } catch (IOException ioe) {
            throw new Exception("Http connection failed.", ioe);
        }
    }

    private void store(InputStream is, int contentLength) throws Exception {
        try {
            getStorage().begin();
			OutputStream output = getStorage().getOutputStream();
			pipe(is, output, contentLength);
			getStorage().commit();
        } catch (IOException ioe) {
            getStorage().rollback();
            throw new Exception("Storage write failed. ", ioe);
        }
    }

	public XMLReader createTransformChain(String[] stylesheets) throws ParserConfigurationException, SAXException, TransformerConfigurationException, UnsupportedEncodingException {
		// Set up to read the input file
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);

		SAXParser parser = spf.newSAXParser();
		XMLReader reader = parser.getXMLReader();
		
		// Create the filters
		// --SAXTransformerFactory is an interface
		// --TransformerFactory is a concrete class
		// --TransformerFactory actually returns a SAXTransformerFactory instance
		// --We didn't care about that before, because we didn't use the
		// --SAXTransformerFactory extensions. But now we do, so we cast the result.
		SAXTransformerFactory stf = (SAXTransformerFactory) TransformerFactory.newInstance();
		XMLFilter filter;
		XMLReader parent = reader; 
		int index = 0;
		while (index < stylesheets.length ) {
			filter = stf.newXMLFilter(new StreamSource(new ByteArrayInputStream(stylesheets[index].getBytes("UTF-8"))));
			filter.setParent(parent);
			parent = filter;
			index++;
		}
		return parent;
	}

    
    private void pipe(InputStream is, OutputStream os, int total) throws IOException {
        int blockSize = 4096;
        int copied = 0;
        int num = 0;
        int logBlockNum = 256; //how many blocks to log progress
        byte[] buf = new byte[blockSize];
        for (int len = -1; (len = is.read(buf)) != -1;) {
            os.write(buf, 0, len);
            if (isKillSendt()) {
                throw new IOException("Download interputed with a kill signal.");
            // every megabyte
            }
            copied += len;
            if (num % logBlockNum == 0) {
                logger.log(Level.INFO, "Downloaded " + copied + "/" + total + " bytes (" + ((double) copied / (double) total * 100) + "%)");
            }
            num++;
        }
        logger.log(Level.INFO, "Download finishes: " + copied + "/" + total + " bytes (" + ((double) copied / (double) total * 100) + "%)");
        os.flush();
    }

	@Override
	public void setStorage(HarvestStorage storage) {
		if (storage instanceof RecordStorage)
			super.setStorage(setupTransformation((RecordStorage) storage));
		else {
			setStatus(HarvestStatus.ERROR);
			resource.setCurrentStatus("Unsupported StorageType: " + storage.getClass().getCanonicalName() + ". Requires RecordStorage");
		}
	}
}
