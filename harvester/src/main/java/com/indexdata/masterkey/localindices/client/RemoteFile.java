package com.indexdata.masterkey.localindices.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class RemoteFile  {

  DecompressStream decompressor; 
  InputStream inputStream; 
  boolean compressed = false;
  String contentType;
  URL url;
  String entry;
  long length;
  File file; 
  boolean isDirectory = false;
  String name;
  String path;
  
  public RemoteFile(URL pathname) /* throws URISyntaxException */ {
    //super(pathname.toURI());    
    this.url = pathname;
    path = url.getPath(); 
    int index = path.lastIndexOf("/");
    if (index == -1)
      name = path;
    name = path.substring(index+1);
  }
  

  public RemoteFile(URL pathname, InputStream is, boolean compressed) /* throws URISyntaxException */  {
    //super(pathname.toURI());
    this.compressed = compressed;
    inputStream = is;
  }

  public RemoteFile(URL pathname, String subentry, InputStream is, boolean compressed) /* throws URISyntaxException */ {
    //super(pathname.toURI());
    url = pathname;
    entry = subentry;
    this.compressed = compressed;
    inputStream = is;
  }

  boolean isCompressed() {
    if (decompressor != null)
      return decompressor.isCompressed();
    return compressed;
  }

  public InputStream getInputStream() throws IOException {
    if (decompressor != null)
      return decompressor.getInputStream();
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
