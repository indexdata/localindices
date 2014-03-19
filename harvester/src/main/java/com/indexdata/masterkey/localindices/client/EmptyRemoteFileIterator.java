package com.indexdata.masterkey.localindices.client;

import java.io.IOException;

public class EmptyRemoteFileIterator implements RemoteFileIterator {

  @Override
  public boolean hasNext() throws IOException {
    return false;
  }

  @Override
  public RemoteFile get() throws IOException {
    return null;
  }

}
