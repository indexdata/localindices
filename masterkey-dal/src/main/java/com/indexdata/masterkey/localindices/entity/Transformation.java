/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.entity;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlTransient;

/**
 * @author Dennis Schafroth
 */
@Entity
@NamedQueries({ @NamedQuery(name = "Transformation.findById", query = "SELECT object(o) FROM Transformation o WHERE o.id = :id") })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
//@Table(name = "transformation")
public class Transformation implements Serializable, Cloneable {

  protected static final long serialVersionUID = 1L;
  // user-set properties
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  protected Long id;
  protected String name;
  @Column(length = 4096)
  protected String description;
  protected Boolean enabled;
  protected Boolean parallel;
  protected String acl;

  @OneToMany(mappedBy = "transformation", cascade = CascadeType.REMOVE)
  @OrderBy("position")
  protected List<TransformationStepAssociation> stepAssociations;

  // Causing troubles for the marshaling
  // @OneToMany(mappedBy="transformation")
  // private Set<Harvestable> harvestables;

  protected Transformation() {
    stepAssociations = new LinkedList<TransformationStepAssociation>();
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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
    if (id == null) return null;
    return id.toString();
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

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // change that so that it check the real class
    if (!(object instanceof Transformation)) {
      return false;
    }
    Transformation other = (Transformation) object;
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

  public List<TransformationStep> getSteps() {
    List<TransformationStep> steps = new LinkedList<TransformationStep>();
    for (TransformationStepAssociation association : stepAssociations) {
      steps.add(association.getStep());
    }
    return steps;
  }

  public void addStepAssociation(TransformationStepAssociation association) {
    association.setTransformation(this);
    stepAssociations.add(association);
  }

  public void addStep(TransformationStep step, int position) {
    TransformationStepAssociation association = new TransformationStepAssociation();
    association.setTransformation(this);
    association.setStep(step);
    association.setPosition(position);
    stepAssociations.add(association);
    // TODO bi-directional?
    // step.getTransformations().add(association);
  }

  public void deleteStep(int associationId) {
    stepAssociations.remove(associationId);
  }

  public List<TransformationStepAssociation> getStepAssociations() {
    return stepAssociations;
  }

  public void setStepAssociations(List<TransformationStepAssociation> stepAssociations) {
    this.stepAssociations = stepAssociations;
  }

  public Boolean getParallel() {
    return parallel;
  }

  public void setParallel(Boolean parallel) {
    this.parallel = parallel;
  }

  public String getAcl() {
    return acl;
  }

  public void setAcl(String acl) {
    this.acl = acl;
  }

}
