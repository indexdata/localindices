package com.indexdata.masterkey.localindices.harvest.job;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;

import com.indexdata.masterkey.localindices.entity.BasicTransformation;
import com.indexdata.masterkey.localindices.entity.CustomTransformationStep;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.XmlTransformationStep;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.harvest.cache.TestDiskCache;
import com.indexdata.masterkey.localindices.harvest.storage.StorageStatus;

public abstract class JobTester extends TestCase {
  
  public JobTester() {
    File directory = new File("diskcache");
    if (!directory.exists())
      directory.mkdir();
    TestDiskCache.setDiskCacheBasePath("diskcache");
  }

  protected Transformation createTransformationFromResources(String [] steps, boolean runParallel) throws IOException {
    Transformation transformation = new BasicTransformation();
    transformation.setParallel(runParallel);
    int index = 0; 
    for (String resource : steps) {
      	TransformationStep step = null;
      	String prefix = "class:";
      	if (resource.startsWith(prefix)) {
      	  String className = resource.substring(prefix.length());
      	  step = new CustomTransformationStep();
      	  step.setName("Step " + index + "(" + className + ")");
      	  step.setCustomClass(className);
      	  step.setScript("{}");
      	}
      	else {
      	  String template = readResource(resource);
      	  step = new XmlTransformationStep();
      	  step.setName("Step " + index);
      	  step.setDescription("Test");
      	  step.setScript(template);
      	}
      	transformation.addStep(step, index++);
    }
    transformation.setId(1l);
    transformation.setName("Test");
    return transformation;
  }

  private String readResource(String resource) throws IOException, UnsupportedEncodingException {
    InputStream input = getClass().getResourceAsStream(resource);
    assertTrue(input != null);
    byte buf[] = new byte[4096];
    ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
    int length = 0;
    int total = 0;
    while ((length = input.read(buf)) != -1) { 
      byteArray.write(buf, 0, length);
      total += length;
    }
    System.out.println("Step " + resource  + " length: " + total );
    String template = byteArray.toString("UTF-8");
    return template;
  }

  protected void checkStorageStatus(StorageStatus storageStatus, long add, long delete, long total) {
    assertTrue("Status not committed: " + storageStatus.getTransactionState(), StorageStatus.TransactionState.Committed == storageStatus.getTransactionState());
    long deletes = storageStatus.getDeletes();
    assertTrue("Deleted records failed. Expected " + delete + " got " + deletes, 
        	new Long(delete).equals(deletes));
    long adds = storageStatus.getAdds();
    assertTrue("Add records failed. Expected " + add + " got " + adds, 
        	new Long(add).equals(adds));
    long totalFound = storageStatus.getTotalRecords();
    assertTrue("Total records failed. Expected " + total + " got " + totalFound, 
        	new Long(total).equals(totalFound));
  }

  protected void emulateJobScheduler(Harvestable resource, RecordHarvestJob job) {
    // Emulate job schedule finish and persist
    job.finishReceived();
    resource.setCurrentStatus(job.getStatus().name());
    assertTrue("Status wrong: " + resource.getCurrentStatus(), resource.getCurrentStatus().equals("OK"));
  }

}
