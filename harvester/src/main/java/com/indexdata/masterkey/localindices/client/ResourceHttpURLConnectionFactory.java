package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

public class ResourceHttpURLConnectionFactory implements HttpURLConnectionFactory {

  Harvestable resource; 
  StorageJobLogger logger; 
  Proxy proxy; 
  ResourceHttpURLConnectionFactory(Harvestable resource, Proxy proxy, StorageJobLogger logger) {
    this.resource = resource;
    this.proxy = proxy;
    this.logger = logger;
  }
  @Override
  public HttpURLConnection createConnection(URL url) throws IOException {
    HttpURLConnection conn;
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

}
