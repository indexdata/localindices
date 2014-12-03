package com.indexdata.masterkey.localindices.client.filefilters;

import java.util.Date;
import java.util.zip.ZipEntry;

public class ZipEntryFilteringInfo implements ItemFilteringInfo {

  ZipEntry entry = null;
  
  public ZipEntryFilteringInfo(ZipEntry entry) {
    this.entry = entry;
  }

  @Override
  public String getName() {
    if (entry != null) {
      return entry.getName();
    } else {
      return null;
    }
  }

  @Override
  public Date getDate() {
    if (entry != null) {
      return new Date(entry.getTime());
    } else {
      return null;
    }
  }

}
