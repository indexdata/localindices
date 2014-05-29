package com.indexdata.masterkey.localindices.client;

import java.io.IOException;

public interface RemoteFileIterator {
  public boolean hasNext() throws IOException;
  public RemoteFile getNext() throws IOException;
}
