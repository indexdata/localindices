package com.indexdata.masterkey.localindices.oaipmh.server.handler;

public interface OaiPmhRequest {
  String getVerb(); 
  String getSet() throws OaiPmhProcotolException;
  String getStartDate();
  String getUntilDate();
  
  String getParameter(String parameter);
  String getParameterValue(String parameter);
  String[] getParameterValues(String parameter);
  String getBaseUrl();

}
