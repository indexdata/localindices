package com.indexdata.masterkey.localindicies.entity;

import java.io.PrintStream;

import com.indexdata.masterkey.localindices.entity.BasicTransformation;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindicies.entity.TestDAOs.EntityTester;

public class TransformationTestHelper implements EntityTester<Transformation> {

	@Override
	public Transformation create() {
        Transformation entity = new BasicTransformation();
        entity.setName("Test Transformation");
        entity.setDescription("Test Description");
        return entity;
	}

	@Override
	public void print(Transformation entity, PrintStream out) {
		out.println(entity.getId() + " Transformation " + entity.getName() + " " + entity.getDescription()); 
	}

	@Override
	public void modify(Transformation entity) {
		entity.setName("Updated Name");
	}

	@Override
	public Long getId(Transformation entity) {
		return entity.getId();
	}

	@Override
	public Transformation clone(Transformation entity)
			throws CloneNotSupportedException {
		return (Transformation) entity.clone();
	}

	@Override
	public void setup() {
	}

	@Override
	public void cleanup() {
	}

}
