package com.indexdata.masterkey.localindices.client;

import java.io.InputStream;

public interface DecompressStream {

  public InputStream getInputStream();
  public boolean isCompressed();
  
}
