package com.indexdata.masterkey.localindices.client.filefilters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

public class FTPFileFilterComposite implements FTPFileFilter{

  List<FTPFileFilter> fileFilters = new ArrayList<FTPFileFilter>();

  public FTPFileFilterComposite(FTPFileFilter... filter) {
    fileFilters.addAll(Arrays.asList(filter));
  }
  
  @Override
  public boolean accept(FTPFile file) {
    for (FTPFileFilter fileFilter : fileFilters) {
      if (!fileFilter.accept(file)) {
        return false;
      }
    }
    return true;
  }

}
