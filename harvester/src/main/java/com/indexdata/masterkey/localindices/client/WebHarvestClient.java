package com.indexdata.masterkey.localindices.client;

import java.net.Proxy;
import java.net.URL;

import com.indexdata.masterkey.localindices.entity.Harvestable;
//import com.indexdata.masterkey.localindices.crawl.CrawlThread;
import com.indexdata.masterkey.localindices.harvest.job.WebHarvestJob;

public class WebHarvestClient extends AbstractHarvestClient {

  WebHarvestJob job;
  Proxy proxy;
  
  WebHarvestClient(Harvestable resource, Proxy proxy) {
    super(resource, proxy, null);
  }
  
  @Override
  public int download(URL url) throws Exception {
    
    return 0;
  }
  
  

}
