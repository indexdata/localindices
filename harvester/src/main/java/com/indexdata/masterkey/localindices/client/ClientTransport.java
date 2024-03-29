package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

public interface ClientTransport {
  public void connect(URL url) throws IOException;
  public RemoteFileIterator get(URL url) throws IOException, ClientTransportError;
  public void setTimeout(Integer seconds);
  public void setFromDate(Date date);
  public void setExcludeFilePattern(String fileNamePattern);
  public void setIncludeFilePattern(String fileNamePattern);
  public void setRecursive(boolean isRecursive);
}
