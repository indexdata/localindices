package com.indexdata.masterkey.localindices.oaipmh.server.handler;

import javax.servlet.http.HttpServletRequest;

public class SimpleOaiPmhRequest implements OaiPmhRequest {

  HttpServletRequest httpRequest;
  public SimpleOaiPmhRequest(HttpServletRequest request) {
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

  @Override
  public String getUrl() {
    StringBuffer url = httpRequest.getRequestURL();
    //String query = httpRequest.getQueryString();
    return url.toString();
  }


  public String getParameter(String name) {
    return httpRequest.getParameter(name);
  }

}
