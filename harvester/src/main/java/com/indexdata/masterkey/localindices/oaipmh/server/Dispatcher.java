package com.indexdata.masterkey.localindices.oaipmh.server;

 import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhProcotolException;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhHandler;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhRequest;

public interface Dispatcher {
  
  
  OaiPmhHandler onRequest(OaiPmhRequest req) throws OaiPmhProcotolException;

}
