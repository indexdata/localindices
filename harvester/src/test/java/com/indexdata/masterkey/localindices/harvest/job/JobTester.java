package com.indexdata.masterkey.localindices.harvest.job;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import com.indexdata.masterkey.localindices.entity.BasicTransformation;
import com.indexdata.masterkey.localindices.entity.BasicTransformationStep;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;

public abstract class JobTester extends TestCase {

  protected Transformation createTransformationFromResources(String [] steps) throws IOException {
    Transformation transformation = new BasicTransformation();
    int index = 0; 
    for (String resource : steps) {
      	InputStream input = getClass().getResourceAsStream(resource);
      	
      	assertTrue(input != null);
      	byte buf[] = new byte[4096];
      	ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
      	int length = 0;
      	@SuppressWarnings("unused")
	int total = 0;
      	while ((length = input.read(buf)) != -1) { 
      	  byteArray.write(buf, 0, length);
      	  total += length;
      	}
      	//System.out.println("Step " + resource  + " length: " + total );
      	String template = byteArray.toString("UTF-8");
      	TransformationStep step = new BasicTransformationStep("Step " + index, "Test", template);
      	transformation.addStep(step, index++);
    }
    transformation.setId(1l);
    transformation.setName("Test");
    return transformation;
  }

}
