/*
 * Copyright (c) 1995-2013, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.cache;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper that prevents streams from being closed (like pesky XML parsers do)
 * @author jakub
 */
public class NonClosableInputStream extends FilterInputStream {
  
  public NonClosableInputStream(InputStream is) {
    super(is);
  }

  /**
   * This method does nothing, to close the stream call #reallyClose
   * @throws IOException 
   */
  @Override
  public void close() throws IOException {
  }
  
  /**
   * Close underlying stream by calling stream#close
   * @throws IOException 
   */
  public void reallyClose() throws IOException {
    super.close();
  }
  
}
