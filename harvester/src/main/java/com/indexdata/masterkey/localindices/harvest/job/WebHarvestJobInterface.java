package com.indexdata.masterkey.localindices.harvest.job;

import com.indexdata.masterkey.localindices.crawl.WebRobotCache;

public interface WebHarvestJobInterface extends HarvestJob {
  
  WebRobotCache getRobotCache();
  
  void setError(String e);
 
}
