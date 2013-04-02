package com.indexdata.masterkey.localindices.client;

import java.net.Proxy;
import java.net.URL;

//import com.indexdata.masterkey.localindices.crawl.CrawlThread;
import com.indexdata.masterkey.localindices.harvest.job.WebHarvestJob;

public class WebHarvestClient implements HarvestClient {

  WebHarvestJob job;
  Proxy proxy;
  
  @Override
  public int download(URL url) throws Exception {
    
    return 0;
  }
  
  

}
