/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.crawl.HTMLPage;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.harvest.storage.Pz2SolrRecordContentHandler;
import com.indexdata.masterkey.localindices.harvest.storage.Record;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.SplitTransformationChainRecordStorageProxy;
import com.indexdata.masterkey.localindices.harvest.storage.TransformationChainRecordStorageProxy;
import com.indexdata.xml.factory.XmlFactory;
import com.indexdata.xml.filter.MessageConsumer;
import com.indexdata.xml.filter.SplitContentHandler;

/**
 * This class handles bulk HTTP download of a single file.
 * 
 * @author Dennis
 * 
 */
public class BulkRecordHarvestJob extends AbstractRecordHarvestJob {

  private SAXTransformerFactory stf = (SAXTransformerFactory) XmlFactory.newTransformerInstance();

  private String error;
  @SuppressWarnings("unused")
  private List<URL> urls = new ArrayList<URL>();
  private XmlBulkResource resource;
  private Proxy proxy;
  private boolean die = false;
  private Templates templates[];
  private int splitSize = 0;
  private int splitDepth = 0;

  public BulkRecordHarvestJob(XmlBulkResource resource, Proxy proxy) {
    this.proxy = proxy;
    this.resource = resource;
    splitDepth = getNumber(resource.getSplitAt(), splitDepth); 
    splitSize  = getNumber(resource.getSplitSize(), splitSize);
    logger = new StorageJobLogger(getClass(), resource);
    this.resource.setMessage(null);
    setStatus(HarvestStatus.valueOf(resource.getCurrentStatus()));
    List<TransformationStep> steps = resource.getTransformation().getSteps();
    templates = new Templates[steps.size()];
    int index = 0;

    String stepInfo = "";
    String stepScript =""; 
    try {
      for (TransformationStep step : steps) {
	stepInfo =  step.getId() + " " + step.getName();
	if (step.getScript() != null) {
	  stepScript = step.getScript();
	  logger.info("Setting up XSLT template for Step: " + stepInfo);
	  templates[index] = stf.newTemplates(new StreamSource(new StringReader(step.getScript())));
	  index++;
	}
	else {
	  logger.warn("Step " + stepInfo + " has not script!");
	}
      }
    } catch (TransformerConfigurationException tce) {
      error = "Failed to build xslt templates: " + stepInfo;
      templates = new Templates[0];
      logger.error("Failed to build XSLT template for Step: " + stepInfo + "Script: " + stepScript);      
      logger.error(error);
      setStatus(HarvestStatus.ERROR);
    }
  }

  private int getNumber(String value, int defaultValue) {
    int number;
    if (value != null && !"".equals(value)) {
      try {
	number = Integer.parseInt(value);
	if (number < 0)
	  number = defaultValue;
	return number;
      } catch (NumberFormatException nfe) {
	logger.warn("Unable to parse number: " + value);
      }
    }
    return defaultValue;
  }

  private Record convert(Source source) throws TransformerException {
    if (source != null) {
      // TODO Need to handle other RecordStore types.
      SAXResult outputTarget = new SAXResult(new Pz2SolrRecordContentHandler(getStorage(), resource
	  .getId().toString()));
      Transformer transformer = stf.newTransformer();
      transformer.transform(source, outputTarget);
    }
    return null;
  }
  
  private void debugSource(Source xmlSource) {
    boolean debug = false;
    if (debug) {
	logger.debug("Transform xml ");
	StreamResult debugOut = new StreamResult(System.out);
	try {
	  stf.newTransformer().transform(xmlSource, debugOut);

	} catch (Exception e) {
	  logger.debug("Unable to print XML: " + e.getMessage());
	}
    }
  }

  private void debugSource(Node xml) {
    debugSource(new DOMSource(xml));
  }

  protected Source transformNode(Source xmlSource) throws TransformerException {
    Transformer transformer;
    if (templates == null)
      return xmlSource;
    for (Templates template : templates) {
      transformer = template.newTransformer();
      DOMResult result = new DOMResult();
      debugSource(xmlSource);
      transformer.transform(xmlSource, result);
      debugSource(result.getNode());
      
      if (result.getNode() == null) {
	logger.warn("transformNode: No Node found");
	xmlSource = new DOMSource();
      } else
	xmlSource = new DOMSource(result.getNode());
    }
    return xmlSource;
  }

  class TransformerConsumer implements MessageConsumer {

    @Override
    public void accept(Node xmlNode) {
      accept(new DOMSource(xmlNode));
    }

    @Override
    public void accept(Source xmlNode) {
      try {
	convert(transformNode(xmlNode));
      } catch (TransformerException e) {
	logger.error("Failed to transform or convert xmlNode: " + e.getMessage() + " " + xmlNode.toString());
	e.printStackTrace();
      }
    }
  }

  private RecordStorage setupTransformation(RecordStorage storage) {
    if (resource.getTransformation() != null && resource.getTransformation().getSteps().size() > 0) {
      boolean split = (splitSize > 0 && splitDepth > 0);
      XMLReader xmlReader;
      try {
	xmlReader = createTransformChain(split);
	if (split) {
	  SplitContentHandler splitHandler = new SplitContentHandler(new TransformerConsumer(), splitDepth, splitSize);
	  xmlReader.setContentHandler(splitHandler);
	  return new SplitTransformationChainRecordStorageProxy(storage, xmlReader);
	}
	return new TransformationChainRecordStorageProxy(storage, xmlReader,
	    new Pz2SolrRecordContentHandler(storage, resource.getId().toString()));

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
      logger.warn("Bulk harvest received kill signal.");
    }
    return die;
  }

  public String getMessage() {
    return error;
  }

  public void run() {
    try {
      // Don't start if we already are in error
      if (getStatus() == HarvestStatus.ERROR) {
	throw new Exception(error);
      }
      // TODO this is different from old behavior. All insert is now done in one
      // commit.
      getStorage().begin();
      setStatus(HarvestStatus.RUNNING);
      if (resource.getOverwrite())
	;
      getStorage().purge();
      downloadList(resource.getUrl().split(" "));
      setStatus(HarvestStatus.FINISHED);
      getStorage().commit();
    } catch (Exception e) {
      try {
	getStorage().rollback();
      } catch (Exception ioe) {
	logger.warn("Roll-back failed.", ioe);
      }
      setStatus(HarvestStatus.ERROR);
      error = e.getMessage();
      resource.setMessage(e.getMessage());
      logger.error("Download failed.", e);
    }
  }

  private void downloadList(String[] urls) throws Exception {
    for (String url : urls) {
      download(new URL(url));
    }
  }

  private void download(URL url) throws Exception {
    logger.info("Starting download - " + url.toString());
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
	// jump page
	if (contentType.startsWith("text/html")) {
	  logger.info("Jump page found at " + url.toString());
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
		logger.warn("Possible sub-link ignored at " + link.toString());
		recursive++;
		continue;
		// possibly a marc file
	      } else {
		logger.warn("Found file at " + link.toString());
		store(conn.getInputStream(), contentLenght);
		proper++;
	      }
	    } else {
	      logger.warn("Dead link (" + responseCode + " at " + link.toString());
	      dead++;
	      continue;
	    }
	  }
	  if (proper == 0) {
	    logger.error("No proper links found at " + url.toString()
		+ ", trash links: " + recursive + ", dead links: " + dead);
	    throw new Exception("No MARC files found at " + url.toString());
	  }
	} else {
	  // setupTransformation()
	  store(conn.getInputStream(), contentLenght);
	  getStorage().setOverwriteMode(false);
	  return;
	}
      } else {
	throw new Exception("Http connection failed. (" + responseCode + ")");
      }
      logger.info("Finished - " + url.toString());
    } catch (IOException ioe) {
      throw new Exception("Http connection failed.", ioe);
    }
  }

  private void store(InputStream is, int contentLength) throws Exception {
    OutputStream output = getStorage().getOutputStream();
    pipe(is, output, contentLength);
  }

  public XMLReader createTransformChain(boolean split) throws ParserConfigurationException, SAXException,
      TransformerConfigurationException, UnsupportedEncodingException {
    // Set up to read the input file
    SAXParserFactory spf = XmlFactory.newSAXParserFactoryInstance();
    SAXParser parser = spf.newSAXParser();
    XMLReader reader = parser.getXMLReader();
    // If split mode, we are just interested in a reader. The transformation is done in transformNode();
    if (split)
      return reader;
    XMLFilter filter;
    XMLReader parent = reader;
    int index = 0;
    while (index < templates.length) {
      filter = stf.newXMLFilter(templates[index]);
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
    int logBlockNum = 256; // how many blocks to log progress
    byte[] buf = new byte[blockSize];
    for (int len = -1; (len = is.read(buf)) != -1;) {
      os.write(buf, 0, len);
      if (isKillSendt()) {
	throw new IOException("Download interputed with a kill signal.");
	// every megabyte
      }
      copied += len;
      if (num % logBlockNum == 0) {
	logger.info("Downloaded " + copied + "/" + total + " bytes ("
	    + ((double) copied / (double) total * 100) + "%)");
      }
      num++;
    }
    logger.info("Download finishes: " + copied + "/" + total + " bytes ("
	+ ((double) copied / (double) total * 100) + "%)");
    os.flush();
  }

  @Override
  public void setStorage(HarvestStorage storage) {
    if (storage instanceof RecordStorage)
      super.setStorage(setupTransformation((RecordStorage) storage));
    else {
      setStatus(HarvestStatus.ERROR);
      resource.setCurrentStatus("Unsupported StorageType: " + storage.getClass().getCanonicalName()
	  + ". Requires RecordStorage");
    }
  }
}
