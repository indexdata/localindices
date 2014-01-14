package com.indexdata.masterkey.localindices.scheduler;

import java.util.HashMap;
import java.util.Properties;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.dao.StorageDAO;
import com.indexdata.masterkey.localindices.dao.bean.HarvestableDAOFake;
import com.indexdata.masterkey.localindices.dao.bean.StorageDAOFake;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.job.AbstractJobTest;

public class TestJobScheduler extends AbstractJobTest {
  
  private JobScheduler jobScheduler; 
  private HarvestableDAO harvestableDao = new HarvestableDAOFake();
  private StorageDAO  storageDao = new StorageDAOFake();

  public void testJobScheduler() {
    SchedulerThread schedulerThread = new SchedulerThread(new HashMap<String, Object>(), new Properties());  
    jobScheduler = schedulerThread.getScheduler();
    jobScheduler.setHarvestableDao(harvestableDao);
    jobScheduler.setStorageDao(storageDao);
    
    Thread thread = new Thread(schedulerThread);
    thread.start(); 
    Harvestable entity = new XmlBulkResource("http://localhost:8080/harvester/oaipmh");
    
    harvestableDao.create(entity);
    harvestableDao.command(entity, "start");
    harvestableDao.create(entity);
    
    
    schedulerThread.kill();
    try {
      thread.join();
    } catch (InterruptedException ie) {
      
    }
    
  }

}
