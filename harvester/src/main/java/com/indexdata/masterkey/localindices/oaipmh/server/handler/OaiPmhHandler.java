package com.indexdata.masterkey.localindices.oaipmh.server.handler;


public interface OaiPmhHandler {
  
  OaiPmhResponse handle(OaiPmhRequest request) throws OaiPmhProcotolException;
  public void verifyParameters(OaiPmhRequest request, String[][] parameters);
  

}
