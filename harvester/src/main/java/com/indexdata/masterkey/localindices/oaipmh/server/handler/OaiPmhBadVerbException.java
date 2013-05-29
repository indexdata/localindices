package com.indexdata.masterkey.localindices.oaipmh.server.handler;

public class OaiPmhBadVerbException extends OaiPmhProcotolException {
  
  /**
   * 
   */
  private static final long serialVersionUID = -9080710975357188140L;

  public OaiPmhBadVerbException(String message) {
    super("badVerb", message, null);
  }

  public OaiPmhBadVerbException(String message, String rootCause) {
    super("badVerb", message, rootCause);
  }

}
