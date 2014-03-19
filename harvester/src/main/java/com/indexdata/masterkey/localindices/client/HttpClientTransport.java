package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.crawl.HTMLPage;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.utils.DateUtil;

public class HttpClientTransport implements ClientTransport {

  Logger logger = Logger.getLogger(this.getClass());
  XmlBulkResource resource;
  Date lastRequested;
  HttpURLConnection conn;
  
  public HttpClientTransport(XmlBulkResource resource, Date fromDate) {
    this.resource = resource;
    lastRequested = fromDate;
  }

  @Override
  public void connect(URL url) throws IOException {
     conn = createConnection(url);
     if (resource.getTimeout() != null) {
       conn.setConnectTimeout(1000*resource.getTimeout());
       conn.setReadTimeout(1000*resource.getTimeout());
     }
  }

  private HttpURLConnection createConnection(URL url) throws IOException {
    return (HttpURLConnection) url.openConnection();
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

  private RemoteFileIterator handleJumpPage(HttpURLConnection conn) throws IOException, URISyntaxException 
  {
    HTMLPage jp = new HTMLPage(handleContentEncoding(conn), conn.getURL());
    ArrayList<RemoteFile> files = new ArrayList<RemoteFile>();
    for (URL link : jp.getLinks()) {
      files.add(new RemoteFile(link));
    }    
    return new LinkRemoteFileIterator(files);
  }


  @Override
  public RemoteFileIterator get(URL url) throws IOException, ClientTransportError {
    HttpURLConnection conn = createConnection(url);
    conn.setRequestMethod("GET");
    if (getResource().getAllowCondReq() && (lastRequested != null)) {
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
      String contentType = conn.getContentType();
      if ("text/html".startsWith(contentType)) {
	try { 
	  return handleJumpPage(conn);
	} catch (URISyntaxException ex) {
	  throw new ClientTransportError("URI syntax error ", ex);
	}
      } else {
	// handle content type
	InputStream isDec = handleContentEncoding(conn);
	long length = getContentLength(conn);
	RemoteFile file = new RemoteFile(url, isDec, false);
	file.setContentType(contentType);
	if ("application/x-gzip".equals(contentType)) {
	  isDec = new GZIPInputStream(isDec);
	  //System.out.println("Detecting type from decompressed stream: " + URLConnection.guessContentTypeFromStream(isDec));
	  file = new RemoteFile(url, isDec, true);
	  file.setLength(length);
	  file.setContentType(resource.getExpectedSchema());
	  return new SingleFileIterator(file);
	}
	else if ("application/zip".equals(contentType)) {
	  ZipInputStream zipInput = new ZipInputStream(isDec);
	  return new ZipRemoteFileIterator(url, zipInput, resource.getExpectedSchema());
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

  public XmlBulkResource getResource() {
    return resource;
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

  public Date getLastRequested() {
    return lastRequested;
  }

  public void setLastRequested(Date lastRequested) {
    this.lastRequested = lastRequested;
  }


}
