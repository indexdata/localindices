package com.indexdata.masterkey.localindices.client;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.StringTokenizer;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class FtpClientTransport implements ClientTransport {
  private final StorageJobLogger logger;
  private Date fromDate;
  private FTPClient client = null;
  private boolean isRecursive;

  public FtpClientTransport(StorageJobLogger logger) {
    this.logger = logger;
  }

  @Override
  public void connect(URL ftpUrl) throws IOException {
    client = new FTPClient();
    String host = ftpUrl.getHost();
    int port = (ftpUrl.getPort() != -1 ? ftpUrl.getPort() : ftpUrl.getDefaultPort());
    try {
      client.connect(host,port);
      String serverReply = (client.getReplyString()==null ? "" : " Server replied: " + client.getReplyString().split("\n")[0] + ")");
      if (client.isConnected()) {
        logger.debug("Client is connected to " + host + serverReply);
      } else {
        logger.error("Error connecting to " + ftpUrl.toString() + serverReply);
        throw new IOException("Error connecting to " + ftpUrl.toString() + serverReply);
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
      String user = "";
      String pw = "";
      StringTokenizer tokens = new StringTokenizer(userInfo, ":");
      if (tokens.hasMoreTokens()) user = tokens.nextToken();
      if (tokens.hasMoreTokens()) pw = tokens.nextToken();
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

  public String appendPath(String path, String name) {
    if (path.endsWith("/"))
      return path + name;
    else
      return path + "/" + name;
  }

  @Override
  public synchronized RemoteFileIterator get(URL url) throws IOException,
      ClientTransportError {
    if (client == null)
      throw new ClientTransportError("FTP client must be initialized with a call to #connect");
    login(url.getUserInfo());
    FTPFile[] files = client.listFiles(url.getPath());
    if (files.length==0) {
      logger.warn("Did not find any files in " + url.getPath());
    } else {
      logger.debug("Found " + files.length + " files in " + url.getPath());
    }
    return new FtpRemoteFileIterator(client, url, files);
  }

  @Override
  public void setTimeout(Integer seconds) {
  }

  @Override
  public void setCompressedFormat(String mimetype) {
  }

  @Override
  public void setFromDate(Date date) {
    fromDate = date;
  }

  @Override
  public void setRecursive(boolean isRecursive) {
    this.isRecursive = isRecursive;
  }
  
 

}
