package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class FtpRemoteDirectory extends RemoteFile { 
  FTPFile directory;
  FTPClient client;
  
  public FtpRemoteDirectory(URL parent, FTPFile file, FTPClient client) throws MalformedURLException, IOException {
    super(new URL(parent, file.getName() + "/"));
    directory = file;
    this.client = client;
  }

  public boolean isDirectory() {
    
    return true;
  }

  public RemoteFileIterator getFileIterator() throws IOException {
    FTPFile[] files = client.listFiles(url.getPath());
    return new FtpRemoteFileIterator(client, url, files);
  }
}
