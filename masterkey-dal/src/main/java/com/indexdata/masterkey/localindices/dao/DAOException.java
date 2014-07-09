package com.indexdata.masterkey.localindices.dao;

public class DAOException extends Exception {
  private static final long serialVersionUID = 4414864795064821289L;

  public DAOException(String msg) {
    super(msg);
  }

  public DAOException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
