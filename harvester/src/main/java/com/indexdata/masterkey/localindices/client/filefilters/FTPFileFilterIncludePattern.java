package com.indexdata.masterkey.localindices.client.filefilters;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

public class FTPFileFilterIncludePattern implements FTPFileFilter {

  private String pattern = null;

  public FTPFileFilterIncludePattern(String fileNamePattern) {
    pattern = fileNamePattern;
  }
  
  @Override
  public boolean accept(FTPFile file) {
    if (file.isDirectory()) {
      return true;
    } else if (pattern != null && pattern.length()>0) {
      return file.getName().matches(pattern);
    } else {
      return true;
    }
  }
}
