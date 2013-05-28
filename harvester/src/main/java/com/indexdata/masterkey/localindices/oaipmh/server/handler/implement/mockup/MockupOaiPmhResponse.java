package com.indexdata.masterkey.localindices.oaipmh.server.handler.implement.mockup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhResponse;

public class MockupOaiPmhResponse implements OaiPmhResponse {
  /**
   * 
   */
  protected String oaiPmhHeader = "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:oai=\"http://www.openarchives.org/OAI/2.0/\"\n" + 
      "	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:marc=\"http://www.loc.gov/MARC21/slim\"\n" + 
      "	xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/\n" + 
      "          http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n";
  protected String oaiPmhEnd = "</OAI-PMH>\n";

  
  static SimpleDateFormat responseDateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
  static {
    responseDateFormater.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  private static final long serialVersionUID = 5913696993474882233L;

  private StringBuffer response;
  public MockupOaiPmhResponse(String string) {
    response = new StringBuffer(oaiPmhHeader)
		.append("	<responseDate>").append(getResponseDate()).append("</responseDate>\n")
		.append(string).append(oaiPmhEnd);
  }
  
  public String toString() {
    return response.toString();
  }

  @Override
  public String getResponseDate() {
    return responseDateFormater.format(new Date());
  }
}
