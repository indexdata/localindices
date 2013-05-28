package com.indexdata.masterkey.localindices.oaipmh.server.handler.implement.mockup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhHandler;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhRequest;
import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhResponse;

public abstract class CommonOaiPmhHandler implements OaiPmhHandler {

  static SimpleDateFormat requestDateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
  static {
    requestDateFormater.setTimeZone(TimeZone.getTimeZone("UTC"));
  }
  protected String oaiPmhHeader = "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:oai=\"http://www.openarchives.org/OAI/2.0/\"\n" + 
        "	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:marc=\"http://www.loc.gov/MARC21/slim\"\n" + 
        "	xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/\n" + 
        "          http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n";
  protected String oaiPmhEnd = "</OAI-PMH>\n";

  public CommonOaiPmhHandler() {
    
  }

  public String getResponseDate() {     
    String responseDate = 
              "	<responseDate>" + requestDateFormater.format(new Date()) + "</responseDate>\n";
    return responseDate;
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

}
