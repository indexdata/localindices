package com.indexdata.masterkey.localindices.oaipmh.server.handler.implement.mockup;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.oaipmh.server.Dispatcher;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhBadVerbException;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhHandler;

public class MockUpDispatcher implements Dispatcher {

  Map<String, String> dispatchMap = new HashMap<String, String>();
  String packageName = "com.indexdata.masterkey.localindices.oaipmh.server.handler.implement.mockup";

  public MockUpDispatcher() {
    dispatchMap.put("ListIdentifiers", "ListRecords");
    // The following not really needed, since there is a one-to-one between verb and class
    dispatchMap.put("ListRecords", "ListRecords");
    dispatchMap.put("Identify", "Identify");
    dispatchMap.put("ListMetadataPrefix", "ListMetadataPrefix");
    dispatchMap.put("ListRecord", "ListRecord");
    dispatchMap.put("ListSets", "ListSets");
  }

  @SuppressWarnings("unchecked")
  @Override
  public OaiPmhHandler onRequest(HttpServletRequest req) throws OaiPmhBadVerbException {
    
      String[] verbs = req.getParameterValues("verb");
      if (verbs == null || verbs.length !=  1 || !validVerb(verbs)) {
	throw new OaiPmhBadVerbException("Value of the verb argument is not a legal OAI-PMH verb, the verb argument is missing, or the verb argument is repeated.");
      }
      String verb = verbs[0];
      String handlerClassName = dispatchMap.get(verb);
    
    if (handlerClassName == null) {
      Logger.getLogger(this.getClass()).warn("Attempting using '" + verb + "' as class name");
      handlerClassName = verb;
    }
    OaiPmhHandler handler = null;
    try {
      Class<? extends OaiPmhHandler> handlerClass = (Class<? extends OaiPmhHandler>) Class.forName(packageName + "." + handlerClassName);
      handler = handlerClass.newInstance();
      return handler;
    } catch (Exception ex) {
      throw new OaiPmhBadVerbException("No Handler implemented for verb '" + verb + "'");
    }
  }

  private boolean validVerb(String[] verbs) {
    String[] validVerbs = { "Identify", "ListMetadataFormats", "ListSets", "ListIdentifiers",
	"ListRecords", "GetRecord" };

    if (verbs == null || verbs.length != 1)
      return false;
    for (String verb : validVerbs)
      if (verb.equals(verbs[0]))
	return true;
    return false;
  }

}
