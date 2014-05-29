package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.indexdata.masterkey.localindices.harvest.cache.NonClosableInputStream;

public class ZipRemoteFileIterator implements RemoteFileIterator {
  private final URL url;
  private final ZipInputStream zis;
  private ZipEntry zipEntry;
  private final String contentType;
  private boolean closed = false;
  
  public ZipRemoteFileIterator(URL url, ZipInputStream zis, String contentType) throws IOException {
    this.zis = zis;
    this.url = url; 
    this.contentType = contentType;
    this.zipEntry = zis.getNextEntry();
    if (zipEntry == null) {
      closed = true;
      zis.close();
    }
  }

  @Override
  public boolean hasNext() throws IOException {
    if (closed) return false;
    if (zipEntry == null) {
      //signaled by getNext() to retrieve
      zipEntry = zis.getNextEntry();
      if (zipEntry == null) {
        closed = true;
        zis.close();
        return false;
      } else {
        return true;
      }
    } else {
      return true;
    }
  }

  @Override
  public RemoteFile getNext() throws IOException {
    if (closed) return null;
    //we have signaled to next to fetch more but next wasn't called
    if (zipEntry == null) {
      zipEntry = zis.getNextEntry();
      if (zipEntry == null) {
        closed = true;
        zis.close();
        return null;
      }
    }
    RemoteFile file = new RemoteFile(url, zipEntry.getName(), 
      new NonClosableInputStream(zis), false);
    file.setContentType(contentType);
    file.setLength(zipEntry.getSize());
    //signal to hasNext
    zipEntry = null;
    return file;
  }


}
