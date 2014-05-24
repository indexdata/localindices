package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.StringTokenizer;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.entity.XmlBulkResource;

public class FtpClientTransport implements ClientTransport {

  Logger logger = Logger.getLogger(this.getClass());
  XmlBulkResource resource;
  Date fromDate;
  FTPClient client = null;

  public FtpClientTransport(XmlBulkResource resource, Date fromDate) {
    this.resource = resource;
    this.fromDate = fromDate;
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

  private boolean valid(String path) {
    if (path != null && !"".equals(path))
      return true;
    return false;
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
    if (client == null) connect(url);
    login(url.getUserInfo());
    FTPFile[] files = client.listFiles(url.getPath());
    if (files.length==0) {
      logger.warn("Did not find any files in " + url.getPath());
    } else {
      logger.debug("Found " + files.length + " files in " + url.getPath());
    }
    return new FtpRemoteFileIterator(client, url, files);
  }

  public XmlBulkResource getResource() {
    return resource;
  }

  @Override
  public void setTimeout(Integer seconds) {
    // TODO Auto-generated method stub
  }

  @Override
  public void setCompressedFormat(String mimetype) {
  }

  @Override
  public void setFromDate(Date date) {
    fromDate = date;
  }

}
