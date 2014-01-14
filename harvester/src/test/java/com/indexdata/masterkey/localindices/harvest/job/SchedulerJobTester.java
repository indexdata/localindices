package com.indexdata.masterkey.localindices.harvest.job;

import java.util.HashMap;
import java.util.Properties;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.dao.StorageDAO;
import com.indexdata.masterkey.localindices.dao.bean.HarvestableDAOFake;
import com.indexdata.masterkey.localindices.dao.bean.StorageDAOFake;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.scheduler.JobScheduler;
import com.indexdata.masterkey.localindices.scheduler.SchedulerThread;

public class SchedulerJobTester implements JobTester {

  
  private JobScheduler jobScheduler; 
  private HarvestableDAO harvestableDao = new HarvestableDAOFake();
  private StorageDAO  storageDao = new StorageDAOFake();

  @Override
  public RecordHarvestJob doHarvestJob(RecordStorage recordStorage, Harvestable entity) 
  {
    SchedulerThread schedulerThread = new SchedulerThread(new HashMap<String, Object>(), new Properties());  
    JobTesterNotifications notify  = new JobTesterNotifications(schedulerThread); 
    jobScheduler = schedulerThread.getScheduler();
    jobScheduler.setNotifications(notify);

    // Override with fake DAOs
    jobScheduler.setHarvestableDao(harvestableDao);
    jobScheduler.setStorageDao(storageDao);
    Thread thread = new Thread(schedulerThread);
    thread.start(); 
    harvestableDao.create(entity);
    harvestableDao.command(entity, "start");
    // Wait for job to complete and notify 
    try {
      thread.join();
    } catch (InterruptedException ie) {
      
    }
    return notify.job;
  }  
}
