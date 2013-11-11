/*
 * Copyright (c) 1995-2013, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.web.admin.controller;

import javax.faces.bean.ManagedBean;

/**
 *
 * @author jakub
 */
@ManagedBean(name="repoController")
public class RepoController {
  private String connector;

  public String getConnector() {
    return connector;
  }

  public void setConnector(String connector) {
    this.connector = connector;
  }

  public String[] getConnectors() {
    return new String[]{
      "Abari", "Absurdsvanj", "Adjikistan", "Afromacoland",
      "Agrabah", "Agaria", "Aijina", "Ajir", "Al-Alemand",
      "Al Amarja", "Alaine", "Albenistan", "Aldestan",
      "Al Hari", "Alpine Emirates", "Altruria",
      "Allied States of America", "BabaKiueria", "Babalstan",
      "Babar's Kingdom", "Backhairistan", "Bacteria",
      "Bahar", "Bahavia", "Bahkan", "Bakaslavia",
      "Balamkadar", "Baki", "Balinderry", "Balochistan",
      "Baltish", "Baltonia", "Bataniland, Republic of",
      "Bayview", "Banania, Republica de", "Bandrika",
      "Bangalia", "Bangstoff", "Bapetikosweti", "Baracq",
      "Baraza", "Barataria", "Barclay Islands",
      "Barringtonia", "Bay View", "Basenji",};
  }
}
