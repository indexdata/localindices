/*
 * Copyright (c) 1995-2013, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.dao;

/**
 *
 * @author jakub
 */
public class EntityInUse extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = 2222684153147817050L;
  public final static String ERROR_MESSAGE = "entity in use";
  
  public EntityInUse(String message, Throwable cause) {
    super(message, cause);
  }
  
  
}
