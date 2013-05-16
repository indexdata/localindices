package com.indexdata.masterkey.localindices.oaipmh.server;

import javax.servlet.http.HttpServletRequest;

import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhHandler;

public interface Dispatcher {
  
  
  OaiPmhHandler onRequest(HttpServletRequest req);

}
