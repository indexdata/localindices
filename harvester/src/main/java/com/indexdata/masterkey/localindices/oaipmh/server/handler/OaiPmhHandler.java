package com.indexdata.masterkey.localindices.oaipmh.server.handler;


public interface OaiPmhHandler {

  OaiPmhResponse handle(OaiPmhRequest request) throws OaiPmhProcotolException, OaiPmhServerException;
  //void handle(OaiPmhRequest request, OaiPmhResponse response) throws OaiPmhProcotolException;
  public void verifyParameters(OaiPmhRequest request, String[][] parameters);
  
}
