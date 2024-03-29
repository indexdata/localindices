/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.entity;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlTransient;

/**
 * @author Dennis
 */
@Entity
@NamedQueries({ @NamedQuery(name = "TransformationStep.findById", query = "SELECT o FROM TransformationStep o WHERE o.id = :id") })
@Table(name = "STEP")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class TransformationStep implements Serializable, Cloneable {

  protected static final long serialVersionUID = 1L;
  // user-set properties
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  protected Long id;
  protected String name;
  @Column(length = 4096)
  protected String description;

  protected String type;

  protected String inputFormat;
  protected String outputFormat;

  protected Boolean enabled;
  @Lob
  protected String script = "";

  @Lob
  protected String testData = "";
  @Lob
  protected String testOutput = "";

  @OneToMany(mappedBy = "step")
  @OrderBy("position")
  protected List<TransformationStepAssociation> stepAssociations;

  protected String customClass;

  protected String acl;

  public TransformationStep() {
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getScript() {
    return script;
  }

  public void setScript(String script) {
    this.script = script;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  @XmlID
  @XmlElement(name = "id")
  public String getStringId() {
    if (id != null)
      return id.toString();
    return null;
  }

  void setStringId(String id) {
    this.id = Long.parseLong(id);
  }

  @XmlTransient
  public Long getId() {
    return id;
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

  public String getAcl() {
    return acl;
  }

  public void setAcl(String acl) {
    this.acl = acl;
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
    if (!(object instanceof TransformationStep)) {
      return false;
    }
    TransformationStep other = (TransformationStep) object;
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

  public String getInputFormat() {
    return inputFormat;
  }

  public void setInputFormat(String format) {
    this.inputFormat = format;
  }

  public String getOutputFormat() {
    return outputFormat;
  }

  public void setOutputFormat(String format) {
    this.outputFormat = format;
  }

  public String getTestData() {
    return testData;
  }

  public void setTestData(String testData) {
    this.testData = testData;
  }

  public String getTestOutput() {
    return testOutput;
  }

  public void setTestOutput(String testOutput) {
    this.testOutput = testOutput;
  }

  @XmlTransient
  public List<Transformation> getTransformations() {
    List<Transformation> transformations = new LinkedList<Transformation>();
    for (TransformationStepAssociation association : stepAssociations) {
      transformations.add(association.getTransformation());
    }
    return transformations;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setCustomClass(String customClass) {
    this.customClass = customClass;
  }

  public String getCustomClass() {
    return customClass;
  }

}
