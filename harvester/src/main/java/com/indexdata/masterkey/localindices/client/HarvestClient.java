package com.indexdata.masterkey.localindices.client;

import java.net.URL;

public interface HarvestClient {
  
  int download(URL url) throws Exception;
}
