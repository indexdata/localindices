package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.indexdata.masterkey.localindices.harvest.cache.NonClosableInputStream;

public class ZipRemoteFileIterator implements RemoteFileIterator {

  ZipInputStream zip;
  ZipEntry zipEntry = null;
  URL url;
  String contentType; 
  public ZipRemoteFileIterator(URL url, ZipInputStream input, String contentType) {
    zip = input;
    this.url = url; 
    this.contentType = contentType;
  }

  public boolean hasNext() throws IOException {
    try {
      zipEntry = zip.getNextEntry();
      if (zipEntry != null) {
        @SuppressWarnings("unused")
        int method = zipEntry.getMethod();
      }
      else {
	zip.close();
      }
      return zipEntry != null;
    }
    catch (IOException ioe) {
      return false;
    }
  }

  @Override
  public RemoteFile get() throws IOException {
    if (zipEntry != null) {
      RemoteFile file = new RemoteFile(url, zipEntry.getName(), new NonClosableInputStream(zip), false);
      file.setContentType(contentType);
      file.setLength(zipEntry.getSize());
      return file;
    }
    throw new IOException("No Zip Entry found in " + url);
  }


}
