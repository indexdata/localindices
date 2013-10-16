package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

public class AbstractHarvestClient implements HarvestClient {

  protected Harvestable resource;
  protected Proxy proxy; 
  protected StorageJobLogger logger; 
  public AbstractHarvestClient(Harvestable resource, Proxy proxy, StorageJobLogger logger) {
    this.logger = logger;
    this.proxy = proxy;
    this.resource = resource;
  }

  
  @Override
  public int download(URL url) throws Exception {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public HttpURLConnection createConnection(URL url) throws IOException {
    HttpURLConnection conn ; 
    if (proxy != null)
	conn = (HttpURLConnection) url.openConnection(proxy);
    else
	conn = (HttpURLConnection) url.openConnection();
    if (resource.getTimeout() != null) {
	conn.setConnectTimeout(resource.getTimeout() * 1000); 
	conn.setReadTimeout(resource.getTimeout() * 1000);
	logger.info("Configured client connection/read timeout to " + resource.getTimeout());
    }
    return conn; 
  }


  public void setProxy(Proxy newProxy) {
    proxy = newProxy;
  }


  public void setLogger(StorageJobLogger newLogger) {
    logger = newLogger;
  }

}
