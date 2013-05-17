package com.indexdata.masterkey.localindices.oaipmh.server.handler.implement.mockup;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.oaipmh.server.Dispatcher;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhHandler;

public class MockUpDispatcher implements Dispatcher {

  Map<String, String> dispatchMap = new HashMap<String, String>(); 
  String packageName = "com.indexdata.masterkey.localindices.oaipmh.server.handler.implement.mockup"; 

  public MockUpDispatcher() {
    dispatchMap.put("ListRecords", "ListRecords");
  }

  @SuppressWarnings("unchecked")
  @Override
  public OaiPmhHandler onRequest(HttpServletRequest req) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    
    String verb = req.getParameter("verb");
    if (verb == null) 
      	throw new RuntimeException("Missing parameter verb");
    String handlerClassName = dispatchMap.get(verb);
    if (handlerClassName == null) {
      Logger.getLogger(this.getClass()).warn("Attempting using '" + verb + "' as class name");
      handlerClassName = verb;
    }
    Class<? extends OaiPmhHandler> handlerClass = (Class<? extends OaiPmhHandler>) Class.forName(packageName + "." + handlerClassName);
    OaiPmhHandler handler = handlerClass.newInstance();
    if (handler == null)
      throw new RuntimeException("No Handler implemented for verb '" + verb + "'");
    return handler;
  }

}
