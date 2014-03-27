package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class FtpRemoteFileIterator implements RemoteFileIterator {

  FTPClient client;
  URL parent;
  FTPFile[] files;
  int index = 0;
  public FtpRemoteFileIterator(FTPClient client, URL parent, FTPFile[] files) {
    this.client = client;
    this.parent = parent;
    this.files = files;
  }
  
  @Override
  public boolean hasNext() throws IOException {
    return index < files.length;
  }

  @Override
  public synchronized RemoteFile get() throws IOException {
    FTPFile file = files[index++];
    if (file.isDirectory()) {
      return new FtpRemoteDirectory(parent, file, client);
    }
    else 
      return new FtpRemoteFile(parent, file, client);
  }

}
