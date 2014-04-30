package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class FtpRemoteFileIterator implements RemoteFileIterator {

  FTPClient client;
  URL parent;
  List<FTPFile> files = new LinkedList<FTPFile>();
  int index = 0;
  public FtpRemoteFileIterator(FTPClient client, URL parent, FTPFile[] files) {
    this.client = client;
    this.parent = parent;
    for (int index = 0 ; index < files.length; index++)
      if (!(files[index].getName().equals(".") || files[index].getName().equals(".."))) {
	this.files.add(files[index]);
      }
  }
  
  @Override
  public boolean hasNext() throws IOException {
    return index < files.size();
  }

  @Override
  public synchronized RemoteFile get() throws IOException {
    FTPFile file = files.get(index++);
    
    if (file.isDirectory()) {
      return new FtpRemoteDirectory(parent, file, client);
    }
    else 
      return new FtpRemoteFile(parent, file, client);
  }

}
