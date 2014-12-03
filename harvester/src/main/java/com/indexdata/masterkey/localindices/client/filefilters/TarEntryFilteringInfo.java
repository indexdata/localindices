package com.indexdata.masterkey.localindices.client.filefilters;

import java.util.Date;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

public class TarEntryFilteringInfo implements ItemFilteringInfo {

  TarArchiveEntry entry = null;
  public TarEntryFilteringInfo(TarArchiveEntry entry) {
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
      return entry.getLastModifiedDate();
    } else {
      return null;
    }
  }

}
