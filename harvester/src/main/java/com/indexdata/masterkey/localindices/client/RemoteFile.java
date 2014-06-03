package com.indexdata.masterkey.localindices.client;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class RemoteFile  {
  private URL url;
  private String contentType;
  private long length;
  private InputStream inputStream; 
  boolean isDirectory = false;
  private final String name;
  private final String path;
  private final StorageJobLogger logger;
  
  public static final String basename(String path) {
    int slash = path.lastIndexOf("/");
    if (slash == -1)
      return path;
    else
      return path.substring(slash+1);
  }
  
  public RemoteFile(URL url, StorageJobLogger logger) {
    this.url = url;
    this.path = url.getPath(); 
    this.name = basename(path);
    this.logger = logger;
  }

  public RemoteFile(URL url, InputStream is, StorageJobLogger logger) {
    this(url, logger);
    this.inputStream = is;
  }
  
  public RemoteFile(URL url, String name, InputStream is, StorageJobLogger logger) {
    this.url = url;
    this.path = url.getPath();
    this.name = name;
    this.inputStream = is;
    this.logger = logger;
  }

  public InputStream getInputStream() throws IOException {
    return inputStream;
  }

  public String getContentType() throws IOException {
    return contentType;
  }

  public long getLength() {
    return length;
  }

  public void setLength(long length) {
    this.length = length;
  }


  public boolean isDirectory() {
    return isDirectory;
  }

  public void setDirectory(boolean isDirectory) {
    this.isDirectory = isDirectory;
  }


  public void setContentType(String contentType) {
    this.contentType = contentType;
  }
  
  public String getName() {
    return name;
  }

  public String getAbsoluteName() {
    return url.getPath();
  }

  public RemoteFileIterator getIterator() {
    return null;
  }
  
  public URL getURL() {
    return url;
  }
}
