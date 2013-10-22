/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.xml.bind.annotation.XmlID;

/**
 * @author Dennis
 */
@Entity
@NamedQueries({ @NamedQuery(name = "Storage.findById", query = "SELECT o FROM Storage o WHERE o.id = :id") })
//@Table(name = "storage")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Storage implements Serializable, Cloneable {

  protected static final long serialVersionUID = 1L;
  // user-set properties
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  protected Long id;
  protected String name;
  @Column(length = 4096)
  protected String description;
  protected Boolean enabled;
  @Column(length = 4096)
  protected String currentStatus;
  @Column(length = 4096)
  protected String message;
  @Column(length = 100)
  protected String transformation;
  protected String indexingUrl;
  protected String searchUrl;
  @Column(length = 1000)
  protected String customClass;

  /*
   * @OneToMany(mappedBy="storage") // try @XmlTransient private
   * Set<Harvestable> harvestables;
   */

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getCurrentStatus() {
    return currentStatus;
  }

  public void setCurrentStatus(String currentStatus) {
    this.currentStatus = currentStatus;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Long getId() {
    return id;
  }

  @XmlID
  public String getIdAsString() {
    if (id != null)
      return id.toString();
    return "";
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // change that so that it check the real class
    if (!(object instanceof Storage)) {
      return false;
    }
    Storage other = (Storage) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return this.getClass().getCanonicalName() + "[id=" + id + "]";
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  public String getTransformation() {
    return transformation;
  }

  public void setTransformation(String transformation) {
    this.transformation = transformation;
  }

  public String getIndexingUrl() {
    return indexingUrl;
  }

  public String getSearchUrl() {
    return searchUrl;
  }

  public void setUrl(String url) {
    int beginIndex = url.indexOf(";");
    if (beginIndex > 0) {
      this.indexingUrl = url.substring(0, beginIndex-1);
      this.searchUrl = url.substring(beginIndex+1);
    } else {
      indexingUrl = url;
      searchUrl = url;
    }
      
  }

  public abstract String getSearchUrl(Harvestable resource);
  /*
   * public void setHarvestables(Set<Harvestable> harvestables) {
   * this.harvestables = harvestables; }
   * 
   * public Set<Harvestable> getHarvestables() { return harvestables; }
   */
}
