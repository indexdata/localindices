package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.indexdata.masterkey.localindices.client.filefilters.EntryFilter;
import com.indexdata.masterkey.localindices.client.filefilters.ZipEntryFilteringInfo;
import com.indexdata.masterkey.localindices.harvest.cache.NonClosableInputStream;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

public class ZipRemoteFileIterator implements RemoteFileIterator {
  private final StorageJobLogger logger;
  private final URL url;
  private final ZipInputStream zis;
  private ZipEntry zipEntry;
  private final String contentType;
  private boolean closed = false;
  private EntryFilter filter = null;
  
  public ZipRemoteFileIterator(URL url, ZipInputStream zis, String contentType, StorageJobLogger logger, EntryFilter filter) throws IOException {
    this.zis = zis;
    this.url = url; 
    this.contentType = contentType;
    this.filter = filter;
    this.zipEntry = getNextAcceptedEntry(zis,filter);
    if (zipEntry == null) {
      closed = true;
      zis.close();
    }
    this.logger = logger;
  }

  @Override
  public boolean hasNext() throws IOException {
    if (closed) return false;
    if (zipEntry == null) {
      //signaled by getNext() to retrieve
      this.zipEntry = getNextAcceptedEntry(zis,filter);
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
      this.zipEntry = getNextAcceptedEntry(zis,filter);
      if (zipEntry == null) {
        closed = true;
        zis.close();
        return null;
      }
    }
    RemoteFile file = new RemoteFile(url, zipEntry.getName(), 
      new NonClosableInputStream(zis), logger);
    file.setContentType(contentType);
    file.setLength(zipEntry.getSize());
    //signal to hasNext
    zipEntry = null;
    return file;
  }
  
  private ZipEntry getNextAcceptedEntry(ZipInputStream zis, EntryFilter filter) throws IOException {
    ZipEntry zipEntry = null;
    do {
      zipEntry = zis.getNextEntry();
    } while (zipEntry != null && !filter.accept(new ZipEntryFilteringInfo(zipEntry)));
    return zipEntry;
  }
}
