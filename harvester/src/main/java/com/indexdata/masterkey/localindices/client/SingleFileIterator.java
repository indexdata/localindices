package com.indexdata.masterkey.localindices.client;

import java.io.IOException;

public class SingleFileIterator implements RemoteFileIterator {
  private RemoteFile file;
  
  public SingleFileIterator(RemoteFile file) {
    this.file = file;
  }
 
  @Override
  public boolean hasNext() throws IOException {
    return file != null;
  }

  @Override
  public RemoteFile getNext() throws IOException {
    RemoteFile remote = file;
    file = null;
    return remote;
  }

}
