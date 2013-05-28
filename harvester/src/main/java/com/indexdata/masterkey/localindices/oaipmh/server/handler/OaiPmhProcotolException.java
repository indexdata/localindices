package com.indexdata.masterkey.localindices.oaipmh.server.handler;

public class OaiPmhProcotolException extends Exception {

  /**
   * 
   */
  String errorCode; 
  String rootCause;
  
  OaiPmhProcotolException(String errorCode, String message, String rootCause) {
    super(message); 
    this.errorCode = errorCode;
    this.rootCause = rootCause;
  }
  
  private static final long serialVersionUID = -1667855968966193408L;

  String getErrorCode() {
    return errorCode;
  }

  String getRootCause() {
    return rootCause;
  }
  
}
