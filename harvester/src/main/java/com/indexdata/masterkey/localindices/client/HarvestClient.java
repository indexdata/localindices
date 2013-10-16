package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public interface HarvestClient {
  
  int download(URL url) throws Exception;
  HttpURLConnection createConnection(URL url) throws IOException;
}

