package com.indexdata.masterkey.localindices.client;

import java.io.IOException;

public class SingleFileIterator implements RemoteFileIterator {

  RemoteFile file;
  public SingleFileIterator(RemoteFile remote) {
    file = remote;
  }
 
  boolean hasMore = true;
  @Override
  public boolean hasNext() throws IOException {
    return hasMore;
  }

  @Override
  public RemoteFile get() throws IOException {
    hasMore = false;
    RemoteFile remote = file;
    file = null;
    return remote;
  }

}
