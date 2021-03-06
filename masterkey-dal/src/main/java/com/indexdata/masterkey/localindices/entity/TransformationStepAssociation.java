package com.indexdata.masterkey.localindices.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.eclipse.persistence.oxm.annotations.XmlInverseReference;

@Entity
@Table(name="TRANSFORMATION_STEP")
@NamedQueries({
    @NamedQuery(name = "tsa.findById", query = "SELECT o FROM TransformationStepAssociation o WHERE o.id = :id"),
    @NamedQuery(name = "tsa.findStepsByTransformationId", query = "SELECT o FROM TransformationStepAssociation o WHERE o.transformation.id = :id") })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@XmlRootElement(name = "transformationStepAssociation")
public class TransformationStepAssociation implements Serializable, Cloneable {
  private static final long serialVersionUID = -8041896324751041180L;
  
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;
  
  @Column(name = "POSITION")
  private int position;
  @ManyToOne
  @JoinColumn(name = "TRANSFORMATION_ID", nullable=false, referencedColumnName = "ID")
  private Transformation transformation;
  @ManyToOne
  @JoinColumn(name = "STEP_ID", nullable=false, referencedColumnName = "ID")
  private TransformationStep step;

  public TransformationStepAssociation() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }
  
  private static class TransformationAdapter extends XmlAdapter<String, Transformation> {

    @Override
    public Transformation unmarshal(String v) throws Exception {
      Long id = Long.parseLong(v);
      Transformation t = new Transformation();
      t.setId(id);
      return t;
    }

    @Override
    public String marshal(Transformation v) throws Exception {
      return v.getStringId();
    }
    
  }
  
  @XmlJavaTypeAdapter(TransformationAdapter.class)
  public Transformation getTransformation() {
    return transformation;
  }

  public void setTransformation(Transformation transformation) {
    this.transformation = transformation;
  }
  
  public TransformationStep getStep() {
    return step;
  }
  
  public void setStep(TransformationStep step) {
    this.step = step;
  }


  public void setPosition(int position) {
    this.position = position;
  }

  public int getPosition() {
    return position;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

}
