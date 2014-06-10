package com.indexdata.masterkey.localindices.client;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class FtpRemoteDirectory extends RemoteFile { 
  FTPFile directory;
  FTPClient client;
  
  public FtpRemoteDirectory(URL parent, 
    FTPFile file, FTPClient client, StorageJobLogger logger) throws MalformedURLException, IOException {
    super(new URL(parent, basename(file.getName()) + "/"), logger);
    this.directory = file;
    this.client = client;
  }

  @Override
  public boolean isDirectory() {
    return true;
  }
  
  public FtpRemoteFileIterator getIterator () {
    String path = url.getPath();
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    logger.debug("Retrieving file list for "+path);
    FTPFile[] files = null;
    try {
      files = client.listFiles(path);
    } catch (IOException e) {
      logger.error("Could not browse FTP directory due to " + e.getMessage());
    }
    if (files == null || files.length==0) {
      logger.warn("Did not find any files at " + path);
    } else {
      logger.debug("Found " + files.length + " file(s) at " + path);
    }
    return new FtpRemoteFileIterator(client, url, files, logger); 
  }
}
