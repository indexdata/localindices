package com.indexdata.masterkey.localindices.client;

import java.io.IOException;

public interface RemoteFileIterator {

  boolean hasNext() throws IOException;
  RemoteFile get() throws IOException;
}
