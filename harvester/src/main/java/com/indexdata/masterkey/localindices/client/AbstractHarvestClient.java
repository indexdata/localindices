package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.cache.DiskCache;
import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

public class AbstractHarvestClient implements HarvestClient {
  protected final RecordHarvestJob job;
  protected final Harvestable resource;
  protected final Proxy proxy; 
  protected final StorageJobLogger logger; 
  protected final DiskCache diskCache;
  
  public AbstractHarvestClient(Harvestable resource, RecordHarvestJob job,
    Proxy proxy, StorageJobLogger logger, DiskCache diskCache) {
    this.resource = resource;
    this.job = job;
    this.logger = logger;
    this.proxy = proxy;
    this.diskCache = diskCache;
  }

  
  @Override
  public int download(URL url) throws Exception {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public URLConnection createConnection(URL url) throws IOException {
    URLConnection conn ; 
    if (proxy != null)
	conn = url.openConnection(proxy);
    else
	conn = url.openConnection();
    if (resource.getTimeout() != null) {
	conn.setConnectTimeout(resource.getTimeout() * 1000); 
	conn.setReadTimeout(resource.getTimeout() * 1000);
	logger.info("Configured client connection/read timeout to " + resource.getTimeout());
    }
    return conn; 
  }

  public RecordHarvestJob getJob() {
    return job;
  }

  public Harvestable getResource() {
    return resource;
  }

  public Proxy getProxy() {
    return proxy;
  }

  public StorageJobLogger getLogger() {
    return logger;
  }
  

}
