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

  public RemoteFile(URL pathname, InputStream is) {
    this(pathname);
    this.inputStream = is;
  }
  
  public RemoteFile(URL url, String name, InputStream is) {
    this.url = url;
    this.path = url.getPath();
    this.name = name;
    this.inputStream = is;
  }

  public InputStream getInputStream() throws IOException {
    return inputStream;
  }

  public String getContentType() throws IOException {
    if (contentType != null)
      return contentType;
    if (inputStream != null) {
      return URLConnection.guessContentTypeFromStream(getInputStream());
    }
    return null;
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
}
