/*
 * Copyright (c) 1995-2013, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author jakub
 */
public class CachingInputStream extends FilterInputStream {
  private FileOutputStream fos;
  private final boolean isWrite;

  public CachingInputStream(InputStream in, String fileName) throws FileNotFoundException {
    super(in);
    fos = new FileOutputStream(fileName, false);
    isWrite = true;
  }
  
  public CachingInputStream(String fileName) throws FileNotFoundException {
    super(new FileInputStream(fileName));
    isWrite = false;
  }

  @Override
  public int read() throws IOException {
    int b = super.read();
    if (isWrite && b != -1) fos.write(b);
    return b;
  }

  @Override
  public int read(byte[] b) throws IOException {
    int len = super.read(b);
    if (isWrite && len != -1) fos.write(b, 0, len);
    return len;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int l = super.read(b, off, len);
    if (isWrite && l != -1) fos.write(b, off, l);
    return l;
  }

  @Override
  public void close() throws IOException {
    super.close();
    fos.close();
  }
  
  public void closeCache() throws IOException {
    if (fos != null) fos.close();
  }
}
