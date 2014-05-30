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
  private FTPFile file; 
  private FTPClient client;
  private FtpInputStream ftpInputStream;
  
  public FtpRemoteFile(URL parent, FTPFile file, FTPClient client, 
    StorageJobLogger logger) throws MalformedURLException, IOException {
    super(new URL(parent, file.getName()), logger);
    this.file = file;
    this.client = client; 
    this.logger = logger;
  }

  public boolean isDirectory() {
    return false;
  }
  
  public String getName() {
    return file.getName();
  }
  
  public synchronized InputStream getInputStream() throws IOException {
    if (ftpInputStream != null)
      return ftpInputStream;
    client.setFileType(FTP.BINARY_FILE_TYPE);
    InputStream data = client.retrieveFileStream(getAbsoluteName());
    if (data == null) 
      throw new IOException("Error retriving file " + getAbsoluteName());  
    ftpInputStream = new FtpInputStream(data, file.getSize(), client);
    return ftpInputStream;
  }
  
  public long length() {
    return file.getSize();
  }

}
