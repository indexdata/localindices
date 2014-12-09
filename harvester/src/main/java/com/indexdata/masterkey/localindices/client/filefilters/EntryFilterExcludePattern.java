package com.indexdata.masterkey.localindices.client.filefilters;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

public class EntryFilterExcludePattern implements EntryFilter {

  StorageJobLogger logger = null;
  String pattern = null;

  public EntryFilterExcludePattern(String fileNamePattern, StorageJobLogger logger) {
    this.pattern = fileNamePattern;
    this.logger = logger;
  }
  
  @Override
  public boolean accept(EntryFilteringInfo info) {
    if (pattern != null && pattern.length()>0) {
      logger.debug("Exclude pattern " + pattern + (info.getName().matches(pattern) ? " will exclude " : " would keep ") + " "+ info.getName());
      return ! (info.getName().matches(pattern));
    } else {
      return true;
    }
  }
}
