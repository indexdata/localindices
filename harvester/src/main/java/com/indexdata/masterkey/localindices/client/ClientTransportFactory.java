package com.indexdata.masterkey.localindices.client;

import java.net.URL;

public interface ClientTransportFactory {
  
  ClientTransport lookup(URL url);

}
