package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.URL;
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
    client.connect(ftpUrl.getHost(), 
                   (ftpUrl.getPort() != -1 ? ftpUrl.getPort() : ftpUrl.getDefaultPort()));
    if (!client.isConnected())
      throw new IOException("Error connecting to " + ftpUrl.toString());
    else
      logger.debug("Client is connected to " + ftpUrl.getHost());

    String user = "";
    String pw = "";
    String userInfo = ftpUrl.getUserInfo();
    if (userInfo != null) {
      StringTokenizer tokens = new StringTokenizer(userInfo, ":");
      user = tokens.nextToken();
      if (tokens.hasMoreTokens()) {
        pw = tokens.nextToken();
      }
    }
    boolean ok = client.login(user, pw);
    if (!ok) {
      throw new IOException("Failure logging in using un:pw [" + user + "]:[" + pw + "]");
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
    if (client == null)
      connect(url);
    FTPFile[] files = client.listFiles(url.getPath());
    logger.debug("Found " + files.length + " files in " + url.getPath());
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
