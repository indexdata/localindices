package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.StringTokenizer;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPFileFilters;

import com.indexdata.masterkey.localindices.client.filefilters.FTPDateFilter;
import com.indexdata.masterkey.localindices.client.filefilters.FTPFileFilterComposite;
import com.indexdata.masterkey.localindices.client.filefilters.FTPFileFilterExcludePattern;
import com.indexdata.masterkey.localindices.client.filefilters.FTPFileFilterIncludePattern;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

public class FtpClientTransport implements ClientTransport {
  private final StorageJobLogger logger;
  private Date fromDate;
  private String includeFilePattern = null;
  private String excludeFilePattern = null;
  private FTPClient client = null;
  private final boolean usePassive;
  private URL ftpUrl = null;
  private static String DEFAULT_INCLUDE = ".*\\.[Zz][Ii][Pp]|.*\\.[Tt][Aa][Rr]|.*\\.[Gg][Zz]|";

  public FtpClientTransport(StorageJobLogger logger, boolean usePassive) {
    this.logger = logger;
    this.usePassive = usePassive;
  }

  public void reconnect (int wait_ms) throws IOException {
    try {
      Thread.sleep(wait_ms);
    } catch (InterruptedException ie) {
      //
    }
    connect(ftpUrl);
    login(ftpUrl.getUserInfo());
  }
  
  @Override
  public void connect(URL ftpUrl) throws IOException {
    if (client == null) {
      client = new FTPClient();
      this.ftpUrl = ftpUrl;  
    }
    String host = ftpUrl.getHost();
    int port = (ftpUrl.getPort() != -1 ? ftpUrl.getPort() : ftpUrl.getDefaultPort());
    try {
      client.connect(host,port);
      String serverReply = (client.getReplyString() == null
        ? " Server replied: nothing"
        : " Server replied: " + client.getReplyString().split("\n")[0] + ")");
      if (client.isConnected()) {
        logger.debug("Client is connected to " + host + serverReply);
      } else {
        logger.error("Error connecting to " + ftpUrl.toString() + serverReply);
        throw new IOException("Error connecting to " + ftpUrl.toString() + serverReply);
      }
      if (usePassive) {
        client.enterLocalPassiveMode();
        logger.debug("FTP client configured to use passive mode.");
      } else {
        client.enterLocalActiveMode();
        logger.debug("FTP client configured to use active mode.");
      }
        
    } catch (SocketException se) {
      logger.error("Error connecting to host " + host + ", port " + port + ": "+ se.getMessage());
      throw new SocketException("Error connecting to host " + host + ", port " + port + ": "+ se.getMessage());
    } catch (UnknownHostException uhe) {
      logger.debug("Error connecting. Unknown host: " + uhe.getMessage());
      throw new UnknownHostException("Unknown host: "+uhe.getMessage());
    } catch (IOException ioe) {
      logger.debug("Error connecting: " + ioe.getMessage(), ioe);
      throw ioe;
    }
  }

  private void login(String userInfo) throws IOException {
    if (userInfo != null) {
      logger.debug("Logging in to the server with "+userInfo);
      String user = "";
      String pw = "";
      StringTokenizer tokens = new StringTokenizer(userInfo, ":");
      if (tokens.hasMoreTokens()) user = URLDecoder.decode(tokens.nextToken(),"UTF-8");
      if (tokens.hasMoreTokens()) pw = URLDecoder.decode(tokens.nextToken(),"UTF-8");
      boolean ok = client.login(user, pw);
      if (!ok) {
        logger.error("Error logging in to FTP server: " + client.getReplyString());
        throw new IOException("Error logging in to FTP server: " + client.getReplyString());
      }
    } else {
      logger.error("Could not log in to FTP server: No user info provided with FTP URL.");
      throw new IOException("Could not log in to FTP server: No user info provided with FTP URL.");
    }
  }
  
  public FTPClient getClient () {
    return client;
  }

  @Override
  public synchronized RemoteFileIterator get(URL url) throws IOException,
      ClientTransportError {
    if (client == null)
      throw new ClientTransportError("FTP client must be initialized with a call to #connect");
    login(url.getUserInfo());
    //we must strip leading slash!
    String path = url.getPath();
    if (path.startsWith("/")) {
      path = path.substring(1);
    }

    FTPFileFilter dateFilter = (fromDate != null ? new FTPDateFilter(fromDate) : FTPFileFilters.ALL);
    FTPFileFilter excludeFilter = (getExcludeFilePattern() != null ? new FTPFileFilterExcludePattern(getExcludeFilePattern()) : FTPFileFilters.ALL);
    FTPFileFilter includeFilter = (getIncludeFilePattern() != null ? new FTPFileFilterIncludePattern(DEFAULT_INCLUDE+includeFilePattern) : FTPFileFilters.ALL);
    FTPFileFilterComposite fileFilter = new FTPFileFilterComposite(dateFilter,includeFilter,excludeFilter);

    logger.info("Retrieving file list for " + path + 
        (fromDate != null ? " with timestamps after " + fromDate : "") + 
        (getIncludeFilePattern() != null ? " Including only: " + DEFAULT_INCLUDE+getIncludeFilePattern() : "") +
        (getExcludeFilePattern() != null ? " Excluding: " + getExcludeFilePattern() : ""));

    FTPFile[] files =  client.listFiles(path,fileFilter); 

    if (files.length==0) {
      logger.warn("Did not find any files at " + path + 
          (fromDate != null ? " with timestamps after " + fromDate : "") +
          (getIncludeFilePattern() != null ? " Including only: " + DEFAULT_INCLUDE+getIncludeFilePattern() : "") +
          (getExcludeFilePattern() != null ? " Excluding: " + getExcludeFilePattern() : ""));
    } else {
      logger.debug("Found " + files.length + " file(s) at " + path);
    }
    logger.debug("Creating new FTPRemoteFileIterator with url " + url.toString());
    return new FtpRemoteFileIterator(this, url, files, fileFilter, logger);
  }

  @Override
  public void setTimeout(Integer seconds) {
  }


  @Override
  public void setFromDate(Date date) {
    fromDate = date;
  }

  @Override
  public void setRecursive(boolean isRecursive) {
    // not applicable for this transport
  }
  public void setExcludeFilePattern (String fileNamePattern) {
    this.excludeFilePattern = fileNamePattern;
  }
  
  public void setIncludeFilePattern (String fileNamePattern) {
    this.includeFilePattern = fileNamePattern;
  }
  
  private String getExcludeFilePattern () {
    if (excludeFilePattern == null || excludeFilePattern.length()==0) return null;
    else return excludeFilePattern;
  }
  
  private String getIncludeFilePattern () {
    if (includeFilePattern == null || includeFilePattern.length()==0) return null;
    else return includeFilePattern;

  }


}
