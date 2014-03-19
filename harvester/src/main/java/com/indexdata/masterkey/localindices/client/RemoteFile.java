package com.indexdata.masterkey.localindices.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

public class RemoteFile   {

  DecompressStream decompressor; 
  InputStream inputStream; 
  boolean compressed = false;
  String contentType;
  URL url;
  String entry;
  long length;
  File file; 
  boolean isDirectory = false; 
  
  public RemoteFile(URL pathname) throws URISyntaxException {
    //super(pathname.toURI());
    this.url = pathname;
  }
  

  public RemoteFile(URL pathname, InputStream is, boolean compressed) { // throws URISyntaxException {
    //super(pathname.toURI());
    this.compressed = compressed;
    inputStream = is;
  }

  public RemoteFile(URL pathname, String subentry, InputStream is, boolean compressed) { // throws URISyntaxException {
    // super(pathname.toURI());
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

  public InputStream getInputStream() {
    if (decompressor != null)
      return decompressor.getInputStream();
    return inputStream;
  }

  public String getContentType() throws IOException {
    if (contentType != null)
      return contentType;
    return URLConnection.guessContentTypeFromStream(inputStream);
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
}
