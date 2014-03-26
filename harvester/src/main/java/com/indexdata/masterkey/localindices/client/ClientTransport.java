package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

public interface ClientTransport {
  
  void connect(URL url) throws IOException;
  RemoteFileIterator get(URL url) throws IOException, ClientTransportError;
  void setTimeout(Integer seconds);
  void setCompressedFormat(String mimetype);
  void setFromDate(Date date);
}
