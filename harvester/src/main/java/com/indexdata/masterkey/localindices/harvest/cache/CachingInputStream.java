/*
 * Copyright (c) 1995-2013, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.cache;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author jakub
 */
public class CachingInputStream extends FilterInputStream {
  private OutputStream os;
  private final boolean isWrite;

  public CachingInputStream(InputStream in, String fileName) throws FileNotFoundException {
    super(in);
    os = new BufferedOutputStream(new FileOutputStream(fileName, false));
    isWrite = true;
  }
  
  public CachingInputStream(String fileName) throws FileNotFoundException {
    super(new FileInputStream(fileName));
    isWrite = false;
  }

  @Override
  public int read() throws IOException {
    int b = super.read();
    if (isWrite && b != -1) os.write(b);
    return b;
  }

  @Override
  public int read(byte[] b) throws IOException {
    int len = super.read(b);
    if (isWrite && len != -1) os.write(b, 0, len);
    return len;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int l = super.read(b, off, len);
    if (isWrite && l != -1) os.write(b, off, l);
    return l;
  }

  @Override
  public void close() throws IOException {
    super.close();
    os.close();
  }
  
  public void closeCache() throws IOException {
    if (os != null) os.close();
  }
}
