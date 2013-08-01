package com.indexdata.masterkey.localindices.oaipmh.server.handler;

import java.io.IOException;


public interface OaiPmhHandler {

  OaiPmhResponse handle(OaiPmhRequest request) throws OaiPmhProcotolException, OaiPmhServerException, IOException;
  //void handle(OaiPmhRequest request, OaiPmhResponse response) throws OaiPmhProcotolException;
  public void verifyParameters(OaiPmhRequest request, String[][] parameters);
  
}
