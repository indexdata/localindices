package com.indexdata.masterkey.localindices.client.filefilters;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

public class FTPFileFilterExcludePattern implements FTPFileFilter {
  
  private String pattern = null;

  public FTPFileFilterExcludePattern(String fileNamePattern) {
    pattern = fileNamePattern;
  }

  @Override
  public boolean accept(FTPFile file) {
    if (pattern != null && pattern.length()>0) {
      if (file.isDirectory()) {
        return true;
      } else if (file.getName().matches(pattern)) {
        return false;
      } else {
        return true;
      }
    } else {
      return true;
    }
  }

}
