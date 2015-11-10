package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import com.indexdata.masterkey.localindices.client.filefilters.EntryFilter;
import com.indexdata.masterkey.localindices.client.filefilters.TarEntryFilteringInfo;
import com.indexdata.masterkey.localindices.harvest.cache.NonClosableInputStream;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

public class TarRemoteFileIterator implements RemoteFileIterator {
  private final StorageJobLogger logger;
  private final URL url;
  private final TarArchiveInputStream tar;
  private TarArchiveEntry tarEntry = null;
  private final String contentType;
  private boolean closed = false;
  private EntryFilter filter = null;
  
  public TarRemoteFileIterator(URL url, TarArchiveInputStream tar, String contentType, StorageJobLogger logger, EntryFilter filter) throws IOException {
    this.tar = tar;
    this.url = url; 
    this.contentType = contentType;
    this.filter = filter;
    this.tarEntry = getNextAcceptedEntry(tar, filter);
    if (tarEntry == null) {
      closed = true;
      tar.close();
    }
    this.logger = logger;
  }

  @Override
  public boolean hasNext() throws IOException {
    if (closed) return false;
    if (tarEntry == null) {
      //signaled by getNext() to retrieve
      this.tarEntry = getNextAcceptedEntry(tar, filter);
      if (tarEntry == null) {
        closed = true;
        tar.close();
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
    if (tarEntry == null) {
      this.tarEntry = getNextAcceptedEntry(tar, filter);
      if (tarEntry == null) {
        closed = true;
        tar.close();
        return null;
      }
    }
    RemoteFile file = new RemoteFile(url, tarEntry.getName(), 
      new NonClosableInputStream(tar), logger);
    file.setContentType(contentType);
    file.setLength(tarEntry.getSize());
    //signal to hasNext
    tarEntry = null;
    return file;
  }
  
  private TarArchiveEntry getNextAcceptedEntry(TarArchiveInputStream tar, EntryFilter filter) throws IOException {
    TarArchiveEntry tarEntry = null;
    do {
      tarEntry = tar.getNextTarEntry();
    } while (tarEntry != null && !filter.accept(new TarEntryFilteringInfo(tarEntry)));
    return tarEntry;
  }



}
