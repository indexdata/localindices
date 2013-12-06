package com.indexdata.masterkey.localindices.harvest.storage;

public class StorageException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = 6035294410957052193L;

  public StorageException(String message) {
    super(message);
  }

  public StorageException(String message, Throwable e) {
    super(message,e);
  }

}
