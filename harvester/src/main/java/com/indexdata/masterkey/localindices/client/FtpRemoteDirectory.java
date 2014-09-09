package com.indexdata.masterkey.localindices.client;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

public class FtpRemoteDirectory extends RemoteFile { 
  private final FTPFile directory;
  private final FTPFileFilter filter;
  private final FtpClientTransport transport;
  
  public FtpRemoteDirectory(URL parent, FtpClientTransport transport, 
    FTPFile file, FTPFileFilter filter, StorageJobLogger logger) throws MalformedURLException, IOException {
    super(new URL(parent, basename(file.getName()) + "/"), logger);
    this.transport = transport;
    this.directory = file;
    this.filter = filter;
  }

  @Override
  public boolean isDirectory() {
    return true;
  }
  
  @Override
  public FtpRemoteFileIterator getIterator () throws IOException {
    FTPClient client = ((FtpClientTransport) transport).getClient();
    String path = url.getPath();
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    logger.debug("Retrieving file list for "+path);
    FTPFile[] files = null;
    try {
      files = client.listFiles(path, filter);
    } catch (IOException e) {
      logger.error("Could not browse FTP directory due to " + e.getMessage());
      throw e;
    }
    if (files == null || files.length==0) {
      logger.warn("Did not find any files at " + path);
    } else {
      logger.debug("Found " + files.length + " file(s) at " + path);
    }
    return new FtpRemoteFileIterator(transport, url, files, filter, logger); 
  }

  @Override
  public String getName() {
    return directory.getName();
  }
  
  
}
