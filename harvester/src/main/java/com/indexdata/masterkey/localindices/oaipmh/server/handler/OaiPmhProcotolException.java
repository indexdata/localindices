package com.indexdata.masterkey.localindices.oaipmh.server.handler;

import com.indexdata.masterkey.localindices.oaipmh.server.handler.implement.mockup.MockupOaiPmhResponse;

public class OaiPmhProcotolException extends Exception {

  /**
   * 
   */
  String errorCode; 
  String message;
  
  public OaiPmhProcotolException(String errorCode, String message, String rootCause) {
    super(rootCause); 
    this.errorCode = errorCode;
    this.message = message;
  }
  
  private static final long serialVersionUID = -1667855968966193408L;

  String getErrorCode() {
    return errorCode;
  }

  public String toString() {
    StringBuffer response = new StringBuffer();
    response
    	.append("  <request>http://arXiv.org/oai2/TODO</request>\n") 
    	.append("  <error code=\"" + errorCode + "\">")
    	.append(message)
    	.append((getMessage() != null ? "(" + getMessage() + ")" : ""))
    	.append("  </error>\n");
    MockupOaiPmhResponse dummy = new MockupOaiPmhResponse(response.toString());
    return dummy.toString();
  }
}
