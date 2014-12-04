package com.indexdata.masterkey.localindices.client.filefilters;

import java.util.Date;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;


public class FTPDateFilter implements FTPFileFilter {

  private Date fromDate = null;
  
  public FTPDateFilter(Date fromDate) {
    this.fromDate = fromDate;
  }

  @Override
  public boolean accept(FTPFile file) {
    if (file.isDirectory()) {
      return true;
    } else {
      return (fromDate == null || file.getTimestamp().getTime().after(fromDate));
    }
  }

}
