package com.indexdata.masterkey.localindices.client.filefilters;

import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;



public class EntryFilterIncludePattern implements EntryFilter {

  String pattern = null;
  StorageJobLogger logger;
  
  public EntryFilterIncludePattern(String fileNamePattern, StorageJobLogger logger) {
    this.pattern = fileNamePattern;
    this.logger = logger;
  }

  @Override
  public boolean accept(ItemFilteringInfo info) {
    if (pattern != null && pattern.length()>0) {
      logger.debug("Include pattern: " + pattern + ", file name: "+info.getName() + ", matches?: " + info.getName().matches(pattern));
      return info.getName().matches(pattern);
    } else {
      return true;
    }
  }

}
