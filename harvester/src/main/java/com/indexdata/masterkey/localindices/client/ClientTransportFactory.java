package com.indexdata.masterkey.localindices.client;

import java.net.URL;

public interface ClientTransportFactory {
  
  public ClientTransport lookup(URL url) throws ClientTransportError;

}
