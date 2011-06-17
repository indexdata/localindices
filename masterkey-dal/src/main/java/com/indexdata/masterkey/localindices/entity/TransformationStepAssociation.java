package com.indexdata.masterkey.localindices.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
// import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "TRANSFORMATION_TRANSFORMATIONSTEP")
@IdClass(TransformationStepAssociationId.class)
public class TransformationStepAssociation {

	@Id
	private Long id;
	
	// TODO @UniqueConstraint(columnNames="stepId")
	private Long transformationId;
	private Long stepId;
	@Column(name = "POSTION")
	private int position;
	@ManyToOne
	@PrimaryKeyJoinColumn(name = "TRANSFORMATIONID", referencedColumnName = "ID")
	private Transformation transformation;
	@ManyToOne
	@PrimaryKeyJoinColumn(name = "STEPID", referencedColumnName = "ID")
	private TransformationStep step;

	public void setTransformation(Transformation transformation) {
		this.transformation = transformation;
	}

	public Transformation getTransformation() {
		return transformation;
	}

	public void setTransformationId(long transformationId) {
		this.transformationId = transformationId;
	}

	public Long getTransformationId() {
		return transformationId;
	}

	public void setStepId(long stepId) {
		this.stepId = stepId;
	}

	public Long getStepId() {
		return stepId;
	}

	public void setStep(TransformationStep step) {
		this.step = step;
	}

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
}
