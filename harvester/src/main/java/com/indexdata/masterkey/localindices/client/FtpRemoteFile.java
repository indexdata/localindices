package com.indexdata.masterkey.localindices.client;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
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
    if (getAbsoluteName().startsWith("/")) {
      rootRelPath = getAbsoluteName().substring(1);
    } else {
      rootRelPath = getAbsoluteName();
    }
  }

  @Override
  public boolean isDirectory() {
    return false;
  }
  
  @Override
  public synchronized InputStream getInputStream() throws IOException {
    // TODO: Still Needed?!? Makes app hang after reconnect.
    //if (ftpInputStream != null)
    //  return ftpInputStream;
    FTPClient client = ((FtpClientTransport)transport).getClient();
    client.setFileType(FTP.BINARY_FILE_TYPE);
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
