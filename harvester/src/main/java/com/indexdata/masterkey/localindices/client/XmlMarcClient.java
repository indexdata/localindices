package com.indexdata.masterkey.localindices.client;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.marc4j.MarcException;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlWriter;
import org.marc4j.TurboMarcXmlWriter;

import com.indexdata.masterkey.localindices.crawl.HTMLPage;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.job.MimeTypeCharSet;
import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

public class XmlMarcClient implements HarvestClient {
  protected StorageJobLogger logger; 
  private XmlBulkResource resource;
  private Proxy proxy; 
  private RecordHarvestJob harvesterJob;
  
  public XmlMarcClient() {
  }
 
  
  @Override
  public int download(URL url) throws Exception {
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
    return 0;
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
    InputStream  input; 
    boolean useTurboMarc = false;
    // Default MARC-8 encoding, use setEncoding to override
    String encoding = null;
    
    public MarcReadStore(InputStream input) {
      this.input = input;
    }    

    public MarcReadStore(InputStream input, boolean useTurboMarc) {
      this.input = input;
      this.useTurboMarc = useTurboMarc;
    }    

    void setEncoding(String encoding) {
      this.encoding = encoding;
    }
    
    @Override
    public void readAndStore() throws Exception {
      MarcStreamReader reader  = new MarcStreamReader(input, encoding);
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

  private ReadStore lookupCompresssionType(HttpURLConnection conn) throws IOException {
    String contentType = conn.getContentType();
    // InputStream after possible Content-Encoding decoded.
    InputStream inputStreamDecoded = handleContentEncoding(conn);
    long contentLength = getContentLength(conn);
    // Content is being decoded. Not the real length
    if (inputStreamDecoded != conn.getInputStream())
      contentLength = -1;
    if ("application/x-gzip".equals(contentType))
      inputStreamDecoded = new GZIPInputStream(inputStreamDecoded);
    else if ("application/zip".equals(contentType)) {
      logger.warn("Only extracting first entry of ZIP from: " + conn.getURL());
      ZipInputStream zipInput = new ZipInputStream(inputStreamDecoded);
      if (zipInput.getNextEntry() == null)
	logger.error("No file found in URL: " + conn.getURL());
      inputStreamDecoded = zipInput;
    }
    MimeTypeCharSet mimeCharset =  new MimeTypeCharSet(contentType);
    // Expected type overrides content type
    if (resource.getExpectedSchema() != null)
       mimeCharset =  new MimeTypeCharSet(resource.getExpectedSchema());
    if (mimeCharset.isMimeType("application/marc") ||
	// TODO doesn't really make sense
	mimeCharset.isMimeType("application/tmarc")) {
      logger.info("Setting up Binary MARC reader ("  
	  + (mimeCharset.getCharset() != null ? mimeCharset.getCharset() : "default") + ")"
	  + (resource.getExpectedSchema() != null ? 
	      " Override by resource mime-type: " + resource.getExpectedSchema() 
	      : "Content-type: " + contentType));
      
      MarcReadStore readStore = new MarcReadStore(inputStreamDecoded);
      String encoding = mimeCharset.getCharset();
      if (encoding != null) 
	readStore.setEncoding(encoding);
      return readStore;
    }
    
    logger.info("Setting up InputStream reader. "
	+ (contentType != null ? "Content-Type:" + contentType : ""));
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

  private void store(MarcStreamReader reader, long contentLength) throws IOException {
    long index = 0;
    OutputStream output = harvesterJob.getOutputStream();
    MarcWriter writer;
    MimeTypeCharSet mimetypeCharset = new MimeTypeCharSet(resource.getOutputSchema());
    if (mimetypeCharset.isMimeType("application/tmarc")) {
    	writer = new TurboMarcXmlWriter(output, true);
    	logger.info("Setting up Binary MARC to TurboMarc converter");
    }
    else { 
  	logger.info("Setting up Binary MARC to MarcXml converter");
 	writer = new MarcXmlWriter(output, true);
    }
    while (reader.hasNext()) {
      try {
	org.marc4j.marc.Record record = reader.next();
	writer.write(record);
	if (harvesterJob.isKillSent()) {
	  // Close to end the pipe 
	  writer.close();
	  throw new IOException("Download interputed with a kill signal.");
	}
      } catch (MarcException e) {
	logger.error("Got MarcException: " + e.getClass().getCanonicalName() + " " + e.getMessage(),e);
	if (e.getCause() !=null)
	  logger.error("Cause: " + e.getCause().getMessage(), e.getCause());
	if (e.getCause() instanceof EOFException) {
	  logger.warn("Received EOF when reading record # " + index);
	}
	break;
      }
      if ((++index) % 1000 == 0)
	logger.info("Marc record read: " + index);
    }
    writer.close();

    logger.info("Marc record read total: " + index);
  }

  private void store(InputStream is, long contentLength) throws Exception {
    OutputStream output = harvesterJob.getOutputStream();
    pipe(is, output, contentLength);
    output.close();
  }

  /* 
   * Pipe is reading after decompression, so Content-Length does does not match total
   * Any stream that doesn't support valid total should return -1 into. The ProgressLogger should adjust to this
   * 
   */
  private void pipe(InputStream is, OutputStream os, long total) throws IOException {
    int blockSize = 100*1024;
    byte[] buf = new byte[blockSize];
    TotalProgressLogger progress = new TotalProgressLogger(total);
    for (int len = -1; (len = is.read(buf)) != -1;) {
      os.write(buf, 0, len);
      if (harvesterJob.isKillSent()) {
	throw new IOException("Download interputed with a kill signal.");
	// every megabyte
      }
      progress.progress(len);
    }
    progress.end();
    
    os.flush();
  }

  private void handleJumpPage(HttpURLConnection conn) throws Exception 
  {
    HTMLPage jp = new HTMLPage(handleContentEncoding(conn), conn.getURL());
    for (URL link : jp.getLinks()) {
      download(link);
    }    
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

  public void setHarvestJob(RecordHarvestJob parent) {
    harvesterJob = parent;
  }

  public void setProxy(Proxy newProxy) {
    proxy = newProxy;
  }

  public void setLogger(StorageJobLogger newLogger) {
    logger = newLogger;
  }
  
  public void setHarvestable(Harvestable newResource) {
    if (newResource instanceof XmlBulkResource) {
      resource = (XmlBulkResource) newResource;
    } else {
      String errorMsg = new String("XmlMarcClient configured with wrong harvestable type: " + newResource.getClass().getCanonicalName()); 
      if (logger != null)
      	logger.fatal(errorMsg);
      else
    	Logger.getLogger("").fatal(errorMsg);
    }
  }
}
