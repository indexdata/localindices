package com.indexdata.masterkey.localindices.oaipmh.server.handler;

import javax.servlet.http.HttpServletRequest;

public class ServletOaiPmhRequest implements OaiPmhRequest {

  HttpServletRequest httpRequest;
  public ServletOaiPmhRequest(HttpServletRequest request) {
    httpRequest = request; 
  }
  
  
  @Override
  public String getParameterValue(String parameter) {
    String value = httpRequest.getParameter(parameter);
    if (value != null) {
      return parameter + "=\"" + value + "\"";  
    }
    return "";
  }

  public String[] getParameterValues(String parameter) {
    return httpRequest.getParameterValues(parameter);
  }

  @Override
  public String getBaseUrl() {
    StringBuffer url = httpRequest.getRequestURL();
    //String query = httpRequest.getQueryString();
    return url.toString();
  }


  public String getParameter(String name) {
    return httpRequest.getParameter(name);
  }

}
