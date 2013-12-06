package com.indexdata.masterkey.localindices.client;

public class StopException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = 1305027002736398729L;
  
  public StopException(String reason) {
    super(reason);
  }
}
