package com.indexdata.masterkey.localindices.client;

import com.indexdata.masterkey.localindices.crawl.HTMLPage;
import com.indexdata.masterkey.localindices.harvest.job.MimeTypeCharSet;
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

public class HttpClientTransport implements ClientTransport {
  private final StorageJobLogger logger;
  private Date lastFrom;
  private String includeFilePattern;
  private String excludeFilePattern;
  private HttpURLConnection conn;
  private Integer timeout;
  private final ClientTransportFactory clientTransportFactory;
  private boolean isRecursive;
  public final static String USER_AGENT_STRING = "IndexData Masterkey Harvesting Client";

  public HttpClientTransport(ClientTransportFactory clientTransportFactory, StorageJobLogger logger) {
    this.clientTransportFactory = clientTransportFactory;
    this.logger = logger;
  }

  @Override
  public void connect(URL url) throws IOException {
    conn = (HttpURLConnection) url.openConnection();
    conn.setRequestProperty("User-Agent", USER_AGENT_STRING);
    if (timeout != null) {
      conn.setConnectTimeout(timeout);
      conn.setReadTimeout(timeout);
    }
  }

  private InputStream handleContentEncoding(HttpURLConnection conn) throws IOException {
    String contentEncoding = conn.getContentEncoding();
    InputStream inputStream = conn.getInputStream();
    if ("gzip".equals(contentEncoding))
      return new GZIPInputStream(inputStream);
    if ("deflate".equalsIgnoreCase(contentEncoding))
      return new InflaterInputStream(inputStream, new Inflater(true));
    return inputStream;
  }

  private RemoteFileIterator handleJumpPage(HttpURLConnection conn) throws IOException, URISyntaxException, ClientTransportError {
    HTMLPage jp = new HTMLPage(handleContentEncoding(conn), conn.getURL());
    InitializingRemoteFileIteratorIterator itt = new InitializingRemoteFileIteratorIterator();
    for (final URL link : jp.getLinks()) {
      // wrap the actual iterator in a delayed initialization proxy
      NotInitializedRemoteFileIterator it = new NotInitializedRemoteFileIterator() {
        RemoteFileIterator iterator;

        @Override
        public void init() throws IOException {
          try {
            ClientTransport client = clientTransportFactory.lookup(link);
            client.setFromDate(lastFrom);
            client.setIncludeFilePattern(includeFilePattern);
            client.setExcludeFilePattern(excludeFilePattern);
            client.connect(link);
            // we only expect jump pages to be linked directly in the harvester
            // otherwise we would need to protect against back-links, cycles, etc
            client.setRecursive(false);
            iterator = client.get(link);
          } catch (ClientTransportError cte) {
            throw new IOException(cte);
          }
        }

        @Override
        public boolean isInitialized() {
          return iterator != null;
        }

        @Override
        public boolean hasNext() throws IOException {
          return iterator.hasNext();
        }

        @Override
        public RemoteFile getNext() throws IOException {
          return iterator.getNext();
        }
      };
      itt.add(it);
    }
    return itt;
  }

  @Override
  public RemoteFileIterator get(URL url) throws IOException, ClientTransportError {
    if (conn == null)
      throw new ClientTransportError("HTTP client must be initialized with a call to #connect");
    conn.setRequestMethod("GET");
    if (lastFrom != null) {
      try {
        String lastModified = DateUtil.serialize(lastFrom, DateUtil.DateTimeFormat.RFC_GMT);
        logger.debug("Conditional request If-Modified-Since: " + lastModified);
        conn.setRequestProperty("If-Modified-Since", lastModified);
      } catch (ParseException pe) {
        logger.error("Failed to parse last modified date: " + lastFrom);
      }
    }
    conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
    int responseCode = conn.getResponseCode();
    if (responseCode == 200) {
      logger.debug("Response OK (200) at " + url);
      String contentType = conn.getContentType();
      logger.debug("Content-Type: " + contentType + " at " + url);
      MimeTypeCharSet mt = new MimeTypeCharSet(contentType);
      if (mt.isHTML()) {
        if (isRecursive) {
          try {
            logger.debug("Detected HTML jump/index page at " + url);
            return handleJumpPage(conn);
          } catch (URISyntaxException ex) {
            throw new ClientTransportError("URI syntax error ", ex);
          }
        } else {
          logger.debug("Ignoring html page at " + url);
          return new EmptyRemoteFileIterator();
        }
      } else {
        // we do not deal with file-level compression in the transport
        InputStream isDec = handleContentEncoding(conn);
        long length = getContentLength(conn);
        RemoteFile file = new RemoteFile(url, isDec, logger);
        file.setContentType(contentType);
        file.setLength(length);
        return new SingleFileIterator(file);
      }
    } else if (responseCode == 304) {// not-modified
      try {
        logger.debug("Content was not modified since '" + DateUtil.serialize(lastFrom, DateUtil.DateTimeFormat.RFC_GMT) + "', completing.");
      } catch (ParseException pe) {
        throw new RuntimeException("Failed to parse Date: " + lastFrom, pe);
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

  public Integer getTimeout() {
    return timeout;
  }

  @Override
  public void setTimeout(Integer timeout) {
    this.timeout = timeout;
  }

  @Override
  public void setFromDate(Date date) {
    lastFrom = date;
  }

  @Override
  public void setRecursive(boolean isRecursive) {
    this.isRecursive = isRecursive;
  }

  @Override
  public void setExcludeFilePattern(String fileNamePattern) {
    this.excludeFilePattern = fileNamePattern;
  }

  @Override
  public void setIncludeFilePattern(String fileNamePattern) {
    this.includeFilePattern = fileNamePattern;
  }

}
