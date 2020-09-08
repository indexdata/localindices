package com.indexdata.masterkey.localindices.client;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;

public class FtpRemoteFile extends RemoteFile {
  private final StorageJobLogger logger;
  private final FTPFile file; 
  private final String rootRelPath;
  private FtpInputStream ftpInputStream;
  private final FtpClientTransport transport;
  
  public FtpRemoteFile(URL parent, FTPFile file, FtpClientTransport transport, 
    StorageJobLogger logger) throws MalformedURLException, IOException {
    super(new URL(parent, basename(file.getName())), logger);
    this.file = file;    
    this.logger = logger;
    this.transport = transport;
    //FTP needs root relative paths!
    logger.debug("getAbsoluteName() returns " + getAbsoluteName() );
    String parentPath = parent.getPath();
    //if (getAbsoluteName().startsWith("/")) {
    String pathCandidate = pathJoin(parentPath, basename(file.getName()));
    if(pathCandidate.startsWith("/")) {
      pathCandidate = pathCandidate.substring(1);
    }
    rootRelPath = pathCandidate;
  }

  private String pathJoin(String parent, String child) {
    if(parent.endsWith("/")) {
      parent = parent.substring(0, parent.length() - 1);
    }
    if(child.startsWith("/")) {
      child = child.substring(1);
    }
    return parent + "/" + child;
  }

  @Override
  public boolean isDirectory() {
    return false;
  }
  
  @Override
  public synchronized InputStream getInputStream() throws FTPConnectionClosedException, IOException {
    FTPClient client = ((FtpClientTransport)transport).getClient();
    client.setFileType(FTP.BINARY_FILE_TYPE);
    logger.debug("Getting input stream for rootRelPath " + rootRelPath);
    InputStream data = client.retrieveFileStream(rootRelPath);
    if (data == null) {
      String reply = client.getReplyString();
      String msg = "Error retriving file " + rootRelPath;
      if (reply != null) msg += ": " + reply;
      throw new IOException(msg);
    }
    ftpInputStream = new FtpInputStream(data, file.getSize(), client, logger);
    return ftpInputStream;
  }
  
  @Override
  public long getLength() {
    return file.getSize();
  }

}
