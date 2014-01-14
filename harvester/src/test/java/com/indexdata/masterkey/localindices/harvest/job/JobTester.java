package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;

public interface JobTester {
  
  RecordHarvestJob doHarvestJob(RecordStorage storage, Harvestable resource) throws IOException; 

}
