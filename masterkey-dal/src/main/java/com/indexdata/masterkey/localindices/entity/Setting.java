/*
 * Copyright (c) 1995-2013, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.entity;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author jakub
 */
@Entity
@XmlRootElement(name = "setting")
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
public class Setting implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = -6196613367553940805L;
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  protected Long id;
  private String name;
  private String value;
  private String label;

  public Setting() {
  }

  public Setting(Long id, String name, String value, String label) {
    this.id = id;
    this.name = name;
    this.value = value;
    this.label = label;
  }
  
  @XmlTransient
  public String getStringId() {
    if (id == null) return null;
    return id.toString();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Setting)) return false;
    return (((Setting) other).id).compareTo(this.id) == 0;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 59 * hash + (this.id != null ? this.id.hashCode() : 0);
    hash = 59 * hash + (this.name != null ? this.name.hashCode() : 0);
    hash = 59 * hash + (this.value != null ? this.value.hashCode() : 0);
    return hash;
  }
  

}
