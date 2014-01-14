package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;

public class SimpleJobTester implements JobTester {

  @Override
  public RecordHarvestJob doHarvestJob(RecordStorage recordStorage, Harvestable resource) throws IOException {
    AbstractRecordHarvestJob job = new BulkRecordHarvestJob((XmlBulkResource) resource, null, new DummyJobNotifications());
    job.setLogger(new ConsoleStorageJobLogger(job.getClass(), resource));
    job.setStorage(recordStorage);
    job.run();
    return job;
  }

}
