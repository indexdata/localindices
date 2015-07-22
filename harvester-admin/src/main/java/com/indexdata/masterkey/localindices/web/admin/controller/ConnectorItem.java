/*
 * Copyright (c) 1995-2013, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.web.admin.controller;

import java.io.Serializable;

/**
 *
 * @author jakub
 */
public class ConnectorItem implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = 825576410087178114L;
  private String fileName;
  private String displayName;
  private String serviceProvider;
  private String author;
  private String note;

  public ConnectorItem(String fileName, String displayName,
    String serviceProvider, String author, String note) {
    this.fileName = fileName;
    this.displayName = displayName;
    this.serviceProvider = serviceProvider;
    this.author = author;
    this.note = note;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getServiceProvider() {
    return serviceProvider;
  }

  public void setServiceProvider(String serviceProvider) {
    this.serviceProvider = serviceProvider;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  @Override
  public String toString() {
    return fileName;
  }
  
  /*
   * Note: Spurious value change events can fire in the autocomplete
   * component unless the ConnectorItem object in the select list
   * evaluates as equal to its corresponding String representation
   * in the component. 
   */
  public boolean equals(Object o) {
    if (o instanceof ConnectorItem  || o instanceof String) {
      return this.toString().equals(o.toString());
    } else {
      return false;
    }
  }
}
