package com.indexdata.masterkey.localindices.client;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.transform.dom.DOMResult;

import org.marc4j.MarcException;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlWriter;
import org.marc4j.TurboMarcXmlWriter;

import com.indexdata.masterkey.localindices.crawl.HTMLPage;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.job.BulkRecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.MimeTypeCharSet;
import com.indexdata.masterkey.localindices.harvest.job.RecordStorageConsumer;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.cache.CachingInputStream;
import com.indexdata.masterkey.localindices.harvest.cache.DiskCache;
import com.indexdata.masterkey.localindices.harvest.storage.RecordDOMImpl;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.XmlSplitter;
import com.indexdata.utils.DateUtil;
import com.indexdata.xml.filter.SplitContentHandler;
import java.io.File;
import java.io.FileInputStream;
import java.net.Proxy;

import static com.indexdata.utils.TextUtils.joinPath;
import java.io.BufferedInputStream;

public class XmlMarcClient extends AbstractHarvestClient {
  private String errorText = "Failed to download/parse/store : ";
  private String errors = errorText;
  private Date lastRequested;
  private int splitAt = 1;
 
  public XmlMarcClient(XmlBulkResource resource, BulkRecordHarvestJob job,  
    Proxy proxy, StorageJobLogger logger, DiskCache dc, Date lastRequested) {
    super(resource, job, proxy, logger, dc);
    this.lastRequested = lastRequested;
  }

  @Override
  public BulkRecordHarvestJob getJob() {
    return (BulkRecordHarvestJob) job;
  }

  @Override
  public XmlBulkResource getResource() {
    return (XmlBulkResource) resource; 
  }
  
  @Override
  public int download(URL url) throws Exception {
    logger.info("Starting download - " + url.toString());
    try {
      HttpURLConnection conn = createConnection(url);
      conn.setRequestMethod("GET");
      if (getResource().getAllowCondReq() && (lastRequested != null)) {
        String lastModified = 
          DateUtil.serialize(lastRequested, DateUtil.DateTimeFormat.RFC_GMT);
        logger.info("Conditional request If-Modified-Since: "+lastModified);
        conn.setRequestProperty("If-Modified-Since", lastModified);
      }
      conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
      int responseCode = conn.getResponseCode();
      if (responseCode == 200) {
	String contentType = conn.getContentType();
	if ("text/html".equals(contentType)) {
	  return handleJumpPage(conn);
	}
	else {
	  ReadStore readStore = prepareReadStore(conn.getInputStream(), 
            getContentLength(conn), conn.getContentType(), conn.getContentEncoding(), 
             joinPath(diskCache.getJobPath(), diskCache.proposeName()));
	  readStore.readAndStore();
	}
      } else if (responseCode == 304) {//not-modified
        logger.info("Content was not modified since '"+DateUtil.serialize(
          lastRequested, DateUtil.DateTimeFormat.RFC_GMT) + "', completing.");
        return 0;
      } else {
	if (getResource().getAllowErrors()) {
	  setErrors(getErrors() + (url.toString() + " "));
	  return 1;
	}
	else
	  throw new Exception("Http connection failed. (" + responseCode + ")");
      }
      // TODO HACK HACK HACK
      Thread.sleep(2000);
      logger.info("Finished - " + url.toString());
    } catch (Exception ex) {
      if (job.isKillSent())
	  throw ex; 
	if (getResource().getAllowErrors()) {
	  logger.warn(errorText + url.toString() + ". Error: " + ex.getMessage());
	  setErrors(getErrors() + (url.toString() + " "));
	  return 1;
	}
	throw ex;
    }
    return 0;
  }
  
  public int download(File file) throws Exception {
      ReadStore readStore = prepareReadStore(new FileInputStream(file), 
        -1, null, null, null);
      readStore.readAndStore();
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
    StreamIterator iterator;
    
    public MarcReadStore(InputStream input, StreamIterator iterator) {
      this.input = input;
      this.iterator = iterator;
    }    

    public MarcReadStore(InputStream input, StreamIterator iterator, boolean useTurboMarc) {
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
      while (iterator.hasNext()) {
	store(reader, -1);      
      }
    }
  }

  
  class InputStreamReadStore implements ReadStore 
  {
    InputStream input; 
    long contentLength;
    StreamIterator iterator;
    
    public InputStreamReadStore(InputStream input, long contentLength, StreamIterator iterator) {
      this.input = input;
      this.contentLength = contentLength;
      this.iterator = iterator;
    }    
    @Override
    public void readAndStore() throws Exception {
      
      while (iterator.hasNext())
	store(input, contentLength); 
    }
  }

  class StreamIterator {
    int once = 1; 
    public boolean hasNext() throws IOException {
      return once-- > 0;
    }
  }

  class ZipStreamIterator extends StreamIterator {
    ZipInputStream zip; 
    public ZipStreamIterator(ZipInputStream input) {
      zip = input;
    }
    public boolean hasNext() throws IOException {
      ZipEntry zipEntry = zip.getNextEntry();
      if (zipEntry != null) {
  	@SuppressWarnings("unused")
  	int method = zipEntry.getMethod();
      }
      return zipEntry != null;
    }
  }

  private ReadStore prepareReadStore(InputStream isRaw, long contentLength, 
    String contentType, String contentEncoding, String cacheFile) throws IOException {
    // InputStream after possible Content-Encoding decoded.
    InputStream isDec = handleContentEncoding(isRaw, contentEncoding);
    StreamIterator streamIterator = new StreamIterator(); 
    // Content is being decoded. Not the real length
    if (isDec != isRaw)
      contentLength = -1; 
    // handle content type
    if ("application/x-gzip".equals(contentType))
      isDec = new GZIPInputStream(isDec);
    else if ("application/zip".equals(contentType)) {
      ZipInputStream zipInput = new ZipInputStream(isDec) {
        @Override
	public void close() throws IOException {
          //zip contains multile entries, don't close the stream at this point
	}
      };
      streamIterator = new ZipStreamIterator(zipInput);
      isDec = zipInput;
    }
    //buffer reads
    isDec = new BufferedInputStream(isDec);
    //cache responses to filesystem
    if (cacheFile != null) {
      isDec = new CachingInputStream(isDec, cacheFile);
    }
    MimeTypeCharSet mimeCharset =  new MimeTypeCharSet(contentType);
    // Expected type overrides content type
    if (getResource().getExpectedSchema() != null)
       mimeCharset =  new MimeTypeCharSet(getResource().getExpectedSchema());
    
    if (mimeCharset.isMimeType("application/marc") ||
	mimeCharset.isMimeType("application/tmarc")) {
      logger.info("Setting up Binary MARC reader ("  
	  + (mimeCharset.getCharset() != null ? mimeCharset.getCharset() : "default") + ")"
	  + (getResource().getExpectedSchema() != null ? 
	      " Override by resource mime-type: " + getResource().getExpectedSchema() 
	      : "Content-type: " + contentType));
      
      MarcReadStore readStore = new MarcReadStore(isDec, streamIterator);
      String encoding = mimeCharset.getCharset();
      if (encoding != null) 
	readStore.setEncoding(encoding);
      return readStore;
    }
    
    logger.info("Setting up InputStream reader. "
	+ (contentType != null ? "Content-Type:" + contentType : ""));
    return new InputStreamReadStore(isDec, contentLength, streamIterator);
  }

  private InputStream handleContentEncoding(InputStream is, String contentEncoding) throws IOException 
  {
    if ("gzip".equals(contentEncoding))
      return new GZIPInputStream(is);
    if ("deflate".equalsIgnoreCase(contentEncoding))
      return new InflaterInputStream(is, new Inflater(true));
    return is;
  }

  private void store(MarcStreamReader reader, long contentLength) throws IOException {
    long index = 0;
    MarcWriter writer;
    MimeTypeCharSet mimetypeCharset = new MimeTypeCharSet(getResource().getOutputSchema());
    boolean isTurboMarc = false;
    if (mimetypeCharset.isMimeType("application/tmarc")) {
    	isTurboMarc = true;
    	logger.info("Setting up Binary MARC to TurboMarc converter");
    }
    else { 
  	logger.info("Setting up Binary MARC to MarcXml converter");
 	//writer = new MarcXmlWriter(output, true);
    }
    RecordStorage storage = job.getStorage();
    while (reader.hasNext()) {
      try {
	org.marc4j.marc.Record record = reader.next();
	DOMResult result = new DOMResult(); 
	if (isTurboMarc)
	  writer = new TurboMarcXmlWriter(result);
	else 
	  writer = new MarcXmlWriter(result);
	writer.write(record);
	writer.close();
	if (record.getLeader().getTypeOfRecord() == 'd')
	  storage.delete(record.getControlNumber());
	else
	  storage.add(new RecordDOMImpl(record.getControlNumber(), null, result.getNode()));
	
	if (job.isKillSent()) {
	  // Close to end the pipe 
	  writer.close();
	  throw new IOException("Download interruted with a kill signal.");
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
    //writer.close();

    logger.info("Marc record read total: " + index);
  }

  private void store(InputStream is, long contentLength) throws Exception {
    RecordStorage storage = job.getStorage();
    SplitContentHandler handler = new SplitContentHandler(new RecordStorageConsumer(storage, job.getLogger()), 
		getJob().getNumber(getResource().getSplitAt(), splitAt));
    XmlSplitter xmlSplitter = new XmlSplitter(storage, logger, handler);
    xmlSplitter.processDataFromInputStream(is);
  }

  /* 
   * Pipe is reading after decompression, so Content-Length does does not match total
   * Any stream that doesn't support valid total should return -1 into. The ProgressLogger should adjust to this
   * 
   */
  @SuppressWarnings("unused")
  private void pipe(InputStream is, OutputStream os, long total) throws IOException {
    int blockSize = 100*1024;
    byte[] buf = new byte[blockSize];
    TotalProgressLogger progress = new TotalProgressLogger(total);
    for (int len = -1; (len = is.read(buf)) != -1;) {
      os.write(buf, 0, len);
      if (job.isKillSent()) {
	throw new IOException("Download interruted with a kill signal.");
      }
      progress.progress(len);
    }
    progress.end();
    
    os.flush();
  }

  private int handleJumpPage(HttpURLConnection conn) throws Exception 
  {
    HTMLPage jp = new HTMLPage(handleContentEncoding(conn.getInputStream(), 
      conn.getContentEncoding()), conn.getURL());
    int results = 0;
    for (URL link : jp.getLinks()) {
      results += download(link);
    }    
    return results; 
  }

  class TotalProgressLogger {
    long total;
    long copied = 0;
    long num = 0;
    int logBlockNum = 1024; // how many blocks to log progress
    String message = "Downloaded ";
    private long lastPercent;

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
      
      if (total != -1) {
	long newPercent = Math.round((double) copied / (double) total * 100); 
	if (lastPercent != newPercent)
	  logger.info(message + copied + "/" + total + " bytes (" + newPercent + "%)");
	lastPercent = newPercent;
      }
      else
	logger.info(message + copied + " bytes");
    }
  }


  public String getErrors() {
    return errors;
  }


  public void setErrors(String errors) {
    this.errors = errors;
  }
}
