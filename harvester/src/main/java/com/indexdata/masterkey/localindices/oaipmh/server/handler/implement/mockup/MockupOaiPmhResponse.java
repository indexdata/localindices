package com.indexdata.masterkey.localindices.oaipmh.server.handler.implement.mockup;

import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhResponse;

public class MockupOaiPmhResponse implements OaiPmhResponse {
  /**
   * 
   */
  private static final long serialVersionUID = 5913696993474882233L;

  private String response;
  public MockupOaiPmhResponse(String string) {
    response = string;
  }
  
  public String toString() {
    return response;
  }
}
