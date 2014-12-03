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
  public boolean accept(ItemFilteringInfo info) {
    if (pattern != null && pattern.length()>0) {
      logger.debug("Exclude pattern: " + pattern + ", file name: "+info.getName() + ", matches?: " + info.getName().matches(pattern));
      return ! (info.getName().matches(pattern));
    } else {
      return true;
    }
  }
}
