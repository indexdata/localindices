package com.indexdata.masterkey.localindices.oaipmh.server.handler.implement.mockup;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.indexdata.masterkey.localindices.oaipmh.server.Dispatcher;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhHandler;

public class MockUpDispatcher implements Dispatcher {

  Map<String, OaiPmhHandler> dispatchMap = new HashMap<String, OaiPmhHandler>(); 

  // TODO problem with this approach is that every request gets same Handler
  MockUpDispatcher() {
    dispatchMap.put("ListRecords", new ListRecords());
  }

  @Override
  public OaiPmhHandler onRequest(HttpServletRequest req) {
    
    String verb = req.getParameter("verb");
    if (verb == null) 
      	throw new RuntimeException("Missing parameter verb");
    OaiPmhHandler handler = dispatchMap.get(verb);
    if (handler == null)
      throw new RuntimeException("No Handler implemented for verb '" + verb + "'");
    return handler;
  }

}
