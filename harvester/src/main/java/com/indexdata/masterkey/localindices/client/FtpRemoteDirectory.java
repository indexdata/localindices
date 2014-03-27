package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class FtpRemoteDirectory extends RemoteFile { 
  FTPFile directory;
  FTPClient client;
  
  public FtpRemoteDirectory(FTPFile file, FTPClient client) throws MalformedURLException, IOException {
    super(new URL("ftp://localhost" + client.printWorkingDirectory() + "/" + file.getName()));
    directory = file;
    this.client = client;
  }

  public boolean isDirectory() {
    
    return true;
  }

  public RemoteFileIterator getFileIterator() {
    
    return null;
  }
}
