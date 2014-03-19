package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.URL;

public interface ClientTransport {
  
  void connect(URL url) throws IOException;
  RemoteFileIterator get(URL url) throws IOException, ClientTransportError;

}
