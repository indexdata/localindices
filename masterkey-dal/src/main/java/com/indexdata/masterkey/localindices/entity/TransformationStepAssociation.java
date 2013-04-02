package com.indexdata.masterkey.localindices.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@Entity
@Table(name="TRANSFORMATION_STEP", uniqueConstraints = @UniqueConstraint(columnNames = {
    "TRANSFORMATION_ID", "STEP_ID" }))
@NamedQueries({
    @NamedQuery(name = "tsa.findById", query = "SELECT o FROM TransformationStepAssociation o WHERE o.id = :id"),
    @NamedQuery(name = "tsa.findStepsByTransformationId", query = "SELECT o FROM TransformationStepAssociation o WHERE o.transformation.id = :id") })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@XmlRootElement(name = "transformationStepAssociation")
public class TransformationStepAssociation implements Serializable, Cloneable {

  public TransformationStepAssociation() {
    step = null;
    transformation = null;
    id = null;
  }

  private static final long serialVersionUID = -8041896324751041180L;

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  /*
   * @Column(name = "TRANSFORMATION_ID") private Long transformationId;
   * 
   * @Column(name = "STEP_ID") private Long stepId;
   */
  @Column(name = "POSITION")
  private int position;
  @ManyToOne
  @PrimaryKeyJoinColumn(name = "TRANSFORMATION_ID", referencedColumnName = "ID")
  private Transformation transformation;
  @ManyToOne
  @PrimaryKeyJoinColumn(name = "STEP_ID", referencedColumnName = "ID")
  @XmlTransient
  private TransformationStep step;

  public void setTransformation(Transformation transformation) {
    this.transformation = transformation;
  }

  @XmlIDREF
  public Transformation getTransformation() {
    return transformation;
  }

  public Long getTransformationId() {
    if (transformation != null)
      return transformation.getId();
    return null;
  }

  /*
   * public void setStepId(long stepId) { this.stepId = stepId; }
   */

  public Long getStepId() {
    if (step != null)
      return step.getId();
    return null;
  }

  public void setStep(TransformationStep step) {
    this.step = step;
  }

  @Id
  @Column(name = "STEP_ID", nullable = false, insertable = false, updatable = false)
  public TransformationStep getStep() {
    return step;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public int getPosition() {
    return position;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

}
