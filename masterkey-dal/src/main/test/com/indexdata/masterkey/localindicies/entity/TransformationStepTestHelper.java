package com.indexdata.masterkey.localindicies.entity;

import java.io.PrintStream;

import com.indexdata.masterkey.localindices.entity.BasicTransformationStep;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindicies.entity.TestDAOs.EntityTester;

public class TransformationStepTestHelper implements
		EntityTester<TransformationStep> {

	@Override
	public TransformationStep create() {
		TransformationStep entity = new BasicTransformationStep();
		entity.setName("Test Transformation");
        entity.setDescription("Test Description");
        entity.setScript("<?xml version=\"1.0\" ?>");
		return entity;
	}

	@Override
	public void print(TransformationStep entity, PrintStream out) {
		out.println("Step: " + entity.getId() + " " + entity.getName() + " " + entity.getDescription());
	}

	@Override
	public void modify(TransformationStep entity) {
		entity.setName("Updated Test Transformation");
        entity.setDescription("Updated Test Description");
	}

	@Override
	public Long getId(TransformationStep entity) {
		return entity.getId();
	}

	@Override
	public TransformationStep clone(TransformationStep entity)
			throws CloneNotSupportedException {

		return (TransformationStep) entity.clone();
	}

	@Override
	public void setup() {
	}

	@Override
	public void cleanup() {
	}

}
