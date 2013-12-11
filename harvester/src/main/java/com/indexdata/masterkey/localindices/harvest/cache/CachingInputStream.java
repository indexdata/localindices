/*
 * Copyright (c) 1995-2013, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.cache;

import java.io.BufferedOutputStream;
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
  private long writePos;
  private long readPos;
  private long markPos;
  private long markLimit;

  public CachingInputStream(InputStream in, String fileName) throws FileNotFoundException {
    super(in);
    os = new BufferedOutputStream(new FileOutputStream(fileName, false));
  }

  @Override
  public int read() throws IOException {
    int b = super.read();
    if (b != -1) {
      if (writePos == readPos) {
        os.write(b);
        writePos++;
      }
      readPos++;
    }
    return b;
  }

  @Override
  public int read(byte[] b) throws IOException {
    int l = super.read(b);
    if (l != -1) {
      if (writePos == readPos) {
        os.write(b, 0, l);
        writePos += l;
      }
      readPos += l;
    }
    return l;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int l = super.read(b, off, len);
    if (l != -1) {
      if (writePos == readPos) {
        os.write(b, off, l);
        writePos += l;
      }
      readPos += l;
    }
    return l;
  }

  @Override
  public void close() throws IOException {
    super.close();
    os.close();
  }

  @Override
  public long skip(long n) throws IOException {
    long l = super.skip(n);
    readPos+=l;
    return l;
  }

  @Override
  public void mark(int readlimit) {
    super.mark(readlimit);
    markLimit = readlimit;
    markPos = readPos;
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    if (markPos+markLimit <= readPos)
      readPos = markPos;
  }
  
  
  public void closeCache() throws IOException {
    if (os != null) os.close();
  }
}
