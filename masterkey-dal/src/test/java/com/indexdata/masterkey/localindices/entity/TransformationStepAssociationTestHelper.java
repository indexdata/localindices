package com.indexdata.masterkey.localindices.entity;

import java.io.PrintStream;

import com.indexdata.masterkey.localindices.dao.EntityInUse;
import com.indexdata.masterkey.localindices.dao.TransformationDAO;
import com.indexdata.masterkey.localindices.dao.TransformationStepDAO;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.entity.TransformationStepAssociation;
import com.indexdata.masterkey.localindices.entity.TestDAOs.EntityTester;

public class TransformationStepAssociationTestHelper implements
		EntityTester<TransformationStepAssociation> {

	Transformation transformation;
	TransformationStep step;
	TransformationDAO transformationDAO;
	TransformationStepDAO transformationStepDAO;
	TransformationStepTestHelper transformationStepHelper = new TransformationStepTestHelper();
	TransformationTestHelper transformationHelper = new TransformationTestHelper();

	public TransformationStepAssociationTestHelper(TransformationDAO transformationDao, TransformationStepDAO stepDao) 
	{
		transformationDAO = transformationDao;
		transformationStepDAO = stepDao;
	}

	@Override
	public TransformationStepAssociation create() {
		TransformationStepAssociation entity = new TransformationStepAssociation();
		entity.setTransformation(transformation);
		entity.setStep(step);
		entity.setPosition(1);
		return entity;
	}

	@Override
	public void print(TransformationStepAssociation entity, PrintStream out) {
		out.println("TSA: " + entity.getId() + " TransformationId: " + entity.getId() + " StepId: " + entity.getId());
	}

	@Override
	public void modify(TransformationStepAssociation entity) {
		entity.setPosition(2);
	}

	@Override
	public Long getId(TransformationStepAssociation entity) {
		return entity.getId();
	}

	@Override
	public TransformationStepAssociation clone(
			TransformationStepAssociation entity)
			throws CloneNotSupportedException {
		return (TransformationStepAssociation)  entity.clone();
	}

	@Override
	public void setup() {
		transformation = transformationHelper.create();
		transformationDAO.create(transformation);
		step = transformationStepHelper.create();
		transformationStepDAO.create(step);
	}

	@Override
	public void cleanup() {
		transformationDAO.delete(transformation);
		try {
		  transformationStepDAO.delete(step);
		} catch (EntityInUse e) {
		  e.printStackTrace();
		}
	}

}
