package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class RemoteFile  {
  private URL url;
  private String contentType;
  private long length;
  private InputStream inputStream; 
  private boolean compressed = false;
  boolean isDirectory = false;
  private final String name;
  private final String path;
  
  public RemoteFile(URL url) {
    this.url = url;
    this.path = url.getPath(); 
    int slash = path.lastIndexOf("/");
    if (slash == -1)
      name = path;
    else
      name = path.substring(slash+1);
  }

  public RemoteFile(URL pathname, InputStream is, boolean compressed) {
    this(pathname);
    this.inputStream = is;
    this.compressed = compressed;
  }
  
  public RemoteFile(URL url, String name, InputStream is, boolean compressed) {
    this.url = url;
    this.path = url.getPath();
    this.name = name;
    this.inputStream = is;
    this.compressed = compressed;
  }

  boolean isCompressed() {
    return compressed;
  }

  public InputStream getInputStream() throws IOException {
    return inputStream;
  }

  public String getContentType() throws IOException {
    if (contentType != null)
      return contentType;
    inputStream = getInputStream(); 
    if (inputStream != null) {
      return URLConnection.guessContentTypeFromStream(getInputStream());
    }
    return null;
  }
  
  public long length() {
    return length;
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
    if (!isDirectory) {
      throw new RuntimeException( getName() + " is not a directory");
    }
    throw new RuntimeException("Iterator not implemented");
  }
}
