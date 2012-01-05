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
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipInputStream;

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

import org.apache.commons.httpclient.HttpClient;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlWriter;
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
      getStorage().databaseStart(resource.getId().toString(), null);
      if (resource.getOverwrite())
	getStorage().purge();
      setStatus(HarvestStatus.RUNNING);
      downloadList(resource.getUrl().split(" "));
      setStatus(HarvestStatus.FINISHED);
      getStorage().databaseEnd();
      getStorage().commit();
    } catch (Exception e) {
      // Test
      e.printStackTrace();      
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

  MarcWriter writer; 
  private void downloadList(String[] urls) throws Exception {
    writer = new MarcXmlWriter(getStorage().getOutputStream());
    for (String url : urls) {
      download(new URL(url));
    }
    if (writer != null)
      writer.close();
  }

  HttpClient client = new HttpClient();
  
  private void handleJumpPage(HttpURLConnection conn) throws Exception 
  {
    HTMLPage jp = new HTMLPage(handleContentEncoding(conn), conn.getURL());
    for (URL link : jp.getLinks()) {
      download(link);
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
      conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
      int responseCode = conn.getResponseCode();
      if (responseCode == 200) {
	String contentType = conn.getContentType();
	if (contentType.equals("text/html")) {
	  handleJumpPage(conn);
	}
	else {
	  ReadStore readStore = lookupCompresssionType(conn);
	  readStore.readAndStore();
	}
      } else {
	throw new Exception("Http connection failed. (" + responseCode + ")");
      }
      logger.info("Finished - " + url.toString());
    } catch (IOException ioe) {
      throw new Exception("Http connection failed.", ioe);
    }
  }

  private long getContentLength(HttpURLConnection conn) {
    // conn.getContentLength() overruns at 2GB, since the interface returns a integer
    long contentLength = -1;
    try {
      contentLength = Long.parseLong(conn.getHeaderField("Content-Length"));
    } catch (Exception e) {
      logger.error("Error parsing Content-Length: " + conn.getHeaderField("Content-Length"));
      contentLength = -1;
    }
    return contentLength;
  }

  interface ReadStore {
    void readAndStore() throws Exception;
  }
  
  class MarcReadStore implements ReadStore 
  {
    HttpURLConnection conn; 
    public MarcReadStore(HttpURLConnection conn) {
      this.conn = conn; 
    }    
    @Override
    public void readAndStore() throws Exception {
      // Assume MARC-8 encoding for now
      MarcStreamReader reader  = new MarcStreamReader(conn.getInputStream(), "MARC-8");
      reader.setBadIndicators(false);
      store(reader, -1);      
    }
  }

  
  class InputStreamReadStore implements ReadStore 
  {
    InputStream input; 
    long contentLength;
    public InputStreamReadStore(InputStream input, long contentLength) {
      this.input = input;
      this.contentLength = contentLength;
    }    
    @Override
    public void readAndStore() throws Exception {
      store(input, contentLength);      
    }
  }

  private ReadStore lookupCompresssionType(HttpURLConnection conn) throws IOException 
  {
    String contentType = conn.getContentType();
    InputStream inputStreamDecoded = handleContentEncoding(conn);
    long contentLength = getContentLength(conn);
    // Content is being decoded. Not the real length
    if (inputStreamDecoded != conn.getInputStream())
      contentLength = -1;
    if (contentType != null) {
      if (contentType.equals("application/marc")) 
	return new MarcReadStore(conn);      
      if (contentType.endsWith("x-gzip"))
	return new InputStreamReadStore(new GZIPInputStream(inputStreamDecoded), contentLength);
      if (contentType.endsWith("zip")) {
	logger.warn("Only extracting first entry of ZIP from: " + conn.getURL());
	ZipInputStream zipInput = new ZipInputStream(inputStreamDecoded);
	if (zipInput.getNextEntry() == null)
	  logger.error("No file found in URL: " + conn.getURL());
	return new InputStreamReadStore(zipInput,  contentLength);
      }
    }
    return new InputStreamReadStore(inputStreamDecoded, contentLength);
  }

  private InputStream handleContentEncoding(HttpURLConnection conn) throws IOException 
  {
    String contentEncoding = conn.getContentEncoding();
    if ("gzip".equals(contentEncoding))
      return new GZIPInputStream(conn.getInputStream());
    if ("deflate".equalsIgnoreCase(contentEncoding))
      return new InflaterInputStream(conn.getInputStream(), new Inflater(true));
    return conn.getInputStream();
  }

  private void store(MarcStreamReader reader, long contentLength) {
    long index = 0;
    while (reader.hasNext()) {
      org.marc4j.marc.Record record = reader.next();
      writer.write(record);
      if ((++index) % 1000 == 0)
	logger.info("Marc record read: " + index);
    }
    logger.info("Marc record read total: " + index);
  }

  private void store(InputStream is, long contentLength) throws Exception {
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

  class TotalProgressLogger {
    long total;
    long copied = 0;
    long num = 0;
    int logBlockNum = 256; // how many blocks to log progress
    String message = "Downloaded ";

    public TotalProgressLogger(long total) {
      this.total = total;
    }

    void progress(int len) {
      copied += len;
      if (copied == total) {
	message = "Download finished: ";
      }
      if (num % logBlockNum == 0 || copied == total) {
	showProgress();
      }
      num++;
    }
    
    protected void end() {
	message = "Download finished: ";
	showProgress();
    }
    protected void showProgress() {
      if (total != -1)
	logger.info(message + copied + "/" + total + " bytes ("
	    + ((double) copied / (double) total * 100) + "%)");
      else
	logger.info(message + copied + " bytes");
    }
  }
  
  /* 
   * Pipe is reading after decompression, so Content-Length does does not match total
   * Any stream that doesn't support valid total should return -1 into. The ProgressLogger should adjust to this
   * 
   */
  private void pipe(InputStream is, OutputStream os, long total) throws IOException {
    int blockSize = 4096;
    byte[] buf = new byte[blockSize];
    TotalProgressLogger progress = new TotalProgressLogger(total);
    for (int len = -1; (len = is.read(buf)) != -1;) {
      os.write(buf, 0, len);
      if (isKillSendt()) {
	throw new IOException("Download interputed with a kill signal.");
	// every megabyte
      }
      progress.progress(len);
    }
    progress.end();
    
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
