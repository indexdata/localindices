package com.indexdata.masterkey.localindices.oaipmh.server.handler.implement.mockup;

import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhHandler;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhRequest;

public abstract class CommonOaiPmhHandler implements OaiPmhHandler {

  public CommonOaiPmhHandler() {
    
  }

  public void verifyParameters(OaiPmhRequest request, String[][] parameters) {
    /*
    if (request.getParameter("resumptionToken") != null && request.getParameter("verb") != null) {
      return ;
    }
    */
    RuntimeException missingParameter = null; 
    for (String[] oneOption: parameters) {
      missingParameter = null; 
      for (String parameter : oneOption) 
        if (request.getParameter(parameter) == null)
          missingParameter = new RuntimeException("Required parameter '" + parameter + "' missing");
      // Had all parameters in this one. 
      if (missingParameter == null)
        return ; 
    }
    // Order of parameter is important. Last one will determine the error message. 
    if (missingParameter != null)
      throw missingParameter;
  }

  protected String getElement(OaiPmhRequest request) {
    String verb = request.getParameter("verb");
    String element = "<" + verb + ">"; 
    return element; 
  }

  protected String getElementEnd(OaiPmhRequest request) {
    String verb = request.getParameter("verb");
    String element = "</" + verb + ">"; 
    return element; 
  }

}
