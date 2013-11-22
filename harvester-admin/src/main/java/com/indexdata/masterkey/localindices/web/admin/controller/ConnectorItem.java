/*
 * Copyright (c) 1995-2013, Index Datassss
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
  
}
