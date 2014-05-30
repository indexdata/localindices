package com.indexdata.masterkey.localindices.client;

import com.indexdata.masterkey.localindices.crawl.HTMLPage;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import com.indexdata.utils.DateUtil;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipInputStream;

public class HttpClientTransport implements ClientTransport {
  private final StorageJobLogger logger;
  private Date lastRequested;
  private HttpURLConnection conn;
  private Integer timeout;
  private String compressedFormat; 
  private final ClientTransportFactory clientTransportFactory;
  private boolean isRecursive;
  
  public HttpClientTransport(ClientTransportFactory clientTransportFactory, 
    StorageJobLogger logger) {
    this.clientTransportFactory = clientTransportFactory;
    this.logger = logger;
  }

  @Override
  public void connect(URL url) throws IOException {
     conn = (HttpURLConnection) url.openConnection();
     if (timeout != null) {
       conn.setConnectTimeout(timeout);
       conn.setReadTimeout(timeout);
     }
  }

  private InputStream handleContentEncoding(HttpURLConnection conn) throws IOException 
  {
    String contentEncoding = conn.getContentEncoding();
    InputStream inputStream = conn.getInputStream();
    if ("gzip".equals(contentEncoding))
      return new GZIPInputStream(inputStream);
    if ("deflate".equalsIgnoreCase(contentEncoding))
      return new InflaterInputStream(inputStream, new Inflater(true));
    return inputStream;
  }

  private RemoteFileIterator handleJumpPage(HttpURLConnection conn) throws IOException, URISyntaxException, ClientTransportError 
  {
    HTMLPage jp = new HTMLPage(handleContentEncoding(conn), conn.getURL());
    RemoteFileIteratorIterator itt = new RemoteFileIteratorIterator();
    for (URL link : jp.getLinks()) {
      ClientTransport client = clientTransportFactory.lookup(link);
      client.connect(link);
      //we only expect jump pages to be linked directly in the harvester
      //otherwise we would need to protect against back-links, cycles, etc
      client.setRecursive(false);
      RemoteFileIterator iter = client.get(link);
      itt.add(iter);
    }    
    return itt;
  }


  @Override
  public RemoteFileIterator get(URL url) throws IOException, ClientTransportError {
    if (conn == null)
      throw new ClientTransportError("HTTP client must be initialized with a call to #connect");
    conn.setRequestMethod("GET");
    if (lastRequested != null) {
      try {
      String lastModified = DateUtil.serialize(lastRequested, DateUtil.DateTimeFormat.RFC_GMT);
      logger.info("Conditional request If-Modified-Since: " + lastModified);
      conn.setRequestProperty("If-Modified-Since", lastModified);
      } catch (ParseException pe) {
	logger.error("Failed to parse last modified date: " + lastRequested);
      }
    }
    conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
    int responseCode = conn.getResponseCode();
    if (responseCode == 200) {
      logger.debug("Response OK (200) at "+url);
      String contentType = conn.getContentType();
      logger.debug("Content-Type: "+contentType+" at "+url);
      if (contentType != null && contentType.startsWith("text/html")) {
	if (isRecursive) {
          try {
            logger.debug("Detected jump/index page at "+url);
            return handleJumpPage(conn);
          } catch (URISyntaxException ex) {
            throw new ClientTransportError("URI syntax error ", ex);
          }
        } else {
          logger.debug("Ignoring html page at "+url);
          return new EmptyRemoteFileIterator();
        }
      } else {
	InputStream isDec = handleContentEncoding(conn);
	long length = getContentLength(conn);
	RemoteFile file = new RemoteFile(url, isDec, false);
	file.setContentType(contentType);
	if ("application/x-gzip".equals(contentType)) {
	  isDec = new GZIPInputStream(isDec);
	  //System.out.println("Detecting type from decompressed stream: " + URLConnection.guessContentTypeFromStream(isDec));
	  file = new RemoteFile(url, isDec, true);
	  file.setLength(length);
	  file.setContentType(compressedFormat);
	  return new SingleFileIterator(file);
	}
	else if ("application/zip".equals(contentType)) {
	  ZipInputStream zipInput = new ZipInputStream(isDec);
	  return new ZipRemoteFileIterator(url, zipInput, compressedFormat);
	}
	return new SingleFileIterator(file);
      }
    } else if (responseCode == 304) {// not-modified
      try {
	logger.info("Content was not modified since '"
	    + DateUtil.serialize(lastRequested, DateUtil.DateTimeFormat.RFC_GMT) + "', completing.");
      } catch (ParseException pe) {
	throw new RuntimeException("Failed to parse Date: " + lastRequested, pe);
      }
      return new EmptyRemoteFileIterator();
    } else {
      throw new ClientTransportError("Http connection failed. (" + responseCode + ")");
    }
}

  private long getContentLength(HttpURLConnection conn) {
    // conn.getContentLength() overruns at 2GB, since the interface returns a integer
    long contentLength;
    try {
      contentLength = Long.parseLong(conn.getHeaderField("Content-Length"));
    } catch (NumberFormatException e) {
      logger.warn("Problem with parsing Content-Length: " + conn.getHeaderField("Content-Length"));
      contentLength = -1;
    }
    return contentLength;
  }

  public Date getLastRequested() {
    return lastRequested;
  }

  public void setLastRequested(Date lastRequested) {
    this.lastRequested = lastRequested;
  }

  public Integer getTimeout() {
    return timeout;
  }

  @Override
  public void setTimeout(Integer timeout) {
    this.timeout = timeout;
  }

  public String getCompressedFormat() {
    return compressedFormat;
  }

  @Override
  public void setCompressedFormat(String compressedFormat) {
    this.compressedFormat = compressedFormat;
  }

  @Override
  public void setFromDate(Date date) {
    lastRequested = date;
  }

  @Override
  public void setRecursive(boolean isRecursive) {
    this.isRecursive = isRecursive;
  }
 
}
