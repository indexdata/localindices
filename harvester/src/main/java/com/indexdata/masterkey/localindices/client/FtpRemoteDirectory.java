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
    this.directory = file;
    this.client = client;
  }

  @Override
  public boolean isDirectory() {
    return true;
  }
}
