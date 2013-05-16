package com.indexdata.masterkey.localindices.oaipmh.server.handler;

import javax.servlet.http.HttpServletRequest;

public interface OaiPmhHandler {
  
  OaiPmhResponse handle(HttpServletRequest request);

}
