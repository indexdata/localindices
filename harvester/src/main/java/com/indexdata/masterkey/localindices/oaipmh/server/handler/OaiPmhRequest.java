package com.indexdata.masterkey.localindices.oaipmh.server.handler;

public interface OaiPmhRequest {
  
  String getParameter(String parameter);
  String getParameterValue(String parameter);
  String getBaseUrl();

}
