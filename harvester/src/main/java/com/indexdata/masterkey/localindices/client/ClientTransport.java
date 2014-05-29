package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

public interface ClientTransport {
  public void connect(URL url) throws IOException;
  public RemoteFileIterator get(URL url) throws IOException, ClientTransportError;
  public void setTimeout(Integer seconds);
  public void setCompressedFormat(String mimetype);
  public void setFromDate(Date date);
  public void setRecursive(boolean isRecursive);
}
