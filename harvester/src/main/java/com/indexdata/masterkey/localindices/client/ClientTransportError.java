package com.indexdata.masterkey.localindices.client;

public class ClientTransportError extends Exception {

  /**
   * 
   */
  
  public ClientTransportError(String message) {
    super(message);
  }
  
  public ClientTransportError(String message, Throwable ex) {
    super(message, ex);
  }

  
  private static final long serialVersionUID = 236433659337072889L;

}
