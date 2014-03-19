package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public interface HarvestClient {
  
  int download(URL url) throws Exception;
  URLConnection createConnection(URL url) throws IOException;
}

