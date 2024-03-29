/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.scheduler;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.Proxy;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.StorageDAO;
import com.indexdata.masterkey.localindices.entity.HarvestConnectorResource;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.entity.StatusResource;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.entity.WebCrawlResource;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.job.BulkRecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.ConnectorHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.HarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.HarvestStatus;
import com.indexdata.masterkey.localindices.harvest.job.OAIRecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.StatusJob;
import com.indexdata.masterkey.localindices.harvest.job.WebRecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorageFactory;
import com.indexdata.utils.CronLine;
import com.indexdata.utils.CronLineParseException;

/**
 * A JobInstance is one instance of a harvesting job managed by the scheduler.
 * It owns the actual harvesting thread, harvesting object. It also knows when
 * start the harvesting thread and is aware of staus and error changes.
 * 
 * @author heikki
 */
public class JobInstance {

  private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
  private Harvestable harvestable;
  private Thread harvestingThread;
  private HarvestJob harvestJob;
  private CronLine cronLine;
  private CronLine lastCronLine;
  private HarvestStatus lastHarvestStatus;
  private String lastStatusMsg;
  private StorageDAO storageDao; 
  public boolean seen; // for checking what has been deleted
  private boolean enabled = true;
  public JobInstance(Harvestable hable, Proxy proxy, boolean enabled, StorageDAO dao)
      throws IllegalArgumentException {
    this.enabled = enabled;
    storageDao = dao;
    // if cron line is not specified - default to today
    if (hable.getScheduleString() == null || hable.getScheduleString().equals("")) {
      logger.log(Level.INFO, "No schedule specified for the job, will start instantly.");
      cronLine = CronLine.currentCronLine();
      hable.setScheduleString(cronLine.toString());
    } else {
      try {
        cronLine = new CronLine(hable.getScheduleString());
      } catch (CronLineParseException clpe) {
        throw new IllegalArgumentException("Bad schedule string", clpe);
      }
    }
    if (hable instanceof OaiPmhResource) {
      if (cronLine.shortestPeriod() < CronLine.DAILY_PERIOD) {
	// Should move to admin/UI check! 
	// And since some resources do support finer granularity, it's basically wrong
	Calendar cal = Calendar.getInstance();
	int min = cal.get(Calendar.MINUTE);
	int hr = cal.get(Calendar.HOUR_OF_DAY);
        try {
          cronLine = new CronLine(min + " " + hr + " " + "* * *");
        } catch (CronLineParseException clpe) {
          throw new IllegalArgumentException(clpe);
        }
	logger.log(Level.WARN,
	    "Job scheduled with lower than daily granularity. Schedule overridden to " + cronLine);
      }
      harvestJob = new OAIRecordHarvestJob((OaiPmhResource) hable, proxy);
    } else if (hable instanceof XmlBulkResource) {
	harvestJob = new BulkRecordHarvestJob((XmlBulkResource) hable, proxy);
    } else if (hable instanceof WebCrawlResource) {
      harvestJob = new WebRecordHarvestJob((WebCrawlResource) hable, proxy);
    } else if (hable instanceof StatusResource) {
      harvestJob = new StatusJob((StatusResource) hable, proxy);
    } else if (hable instanceof HarvestConnectorResource) {
      // hable.getJobClass();
      try {
	Constructor<?> constructor = Class.forName("com.indexdata.masterkey.localindices.harvest.job.ConnectorHarvestJob").getConstructor(hable.getClass(), Proxy.class);
	harvestJob = (HarvestJob) constructor.newInstance(hable, proxy);
      } catch (Exception e) {
	logger.error("Failed to use reflection", e);
	harvestJob = new ConnectorHarvestJob((HarvestConnectorResource) hable, proxy);
	e.printStackTrace();
      }
    } else {
      throw new IllegalArgumentException("Cannot create instance of the harvester.");
    }
    harvestable = hable;
    lastHarvestStatus = HarvestStatus.valueOf(hable.getCurrentStatus());
    lastStatusMsg = hable.getMessage();
    seen = false;
  }

  public Harvestable getHarvestable() {
    return harvestable;
  }

  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Start the harvesting thread for this job.
   */
  public void start() {
    if (harvestingThread == null || !harvestingThread.isAlive()) {
      harvestingThread = new Thread(harvestJob);
      harvestingThread.setName(harvestJob.getClass().getSimpleName() + "(" + harvestable.getId() + " " + harvestable.getName() +")");
      if (harvestJob != null) {
	harvestJob.setJobThread(harvestingThread);
      	// Refresh storage. The cascading in the persistence layer is currently not working
	if (harvestable.getStorage() != null) {
	  Storage storage = storageDao.retrieveById(harvestable.getStorage().getId());
      	  harvestable.setStorage(storage);
      	  RecordStorage recordStorage = HarvestStorageFactory.getStorage(storage);
      	  recordStorage.setHarvestable(harvestable);
      	  harvestJob.setStorage(recordStorage);
	}
	else {
	  if (!(harvestable instanceof StatusResource)) {
	    logger.error("Running Harvest Job without Storage: " + harvestable);
	  }
	}

      }
      harvestingThread.start();
      if (harvestable.getInitiallyHarvested() == null)
	harvestable.setInitiallyHarvested(new Date());
      harvestable.setLastHarvestStarted(new Date());
    }
  }

  /**
   * Tell the harvesting thread to stop
   */
  public void stop() {
    harvestJob.kill();
  }

  /**
   * Completely remove the harvesting job: tell the thread to stop and destroy
   * the data.
   */
  public void destroy() {
    harvestJob.kill();
    try {
      RecordStorage storage = harvestJob.getStorage(); 
      storage.purge(true);
    } catch (IOException ex) {
      logger.log(Level.ERROR, "Destroy failed.");
      logger.log(Level.DEBUG, ex);
    }
  }

  /**
   * Inform the job that the harvested data was picked up and it may go to
   * sleep.
   */
  public void notifyFinish() {
    harvestJob.finishReceived();
    // TODO Not sure this is a good idea. There are too many ways for a job to finish 
    // that we should let the job decide self.    
    harvestable.setLastHarvestFinished(new Date());
  }

  /**
   * Checks if the time has come to run the harvesting thread.
   * 
   * Changed behavior: Harvest Immediately will run even when disabled.
   * 
   * @return true/false
   */
  public boolean timeToRun() {
    boolean itIsTime = false;
    if (harvestable.getHarvestImmediately()) {
      itIsTime = true;
      harvestable.setHarvestImmediately(false);
    } else if (harvestable.getEnabled()) {
      CronLine curCron = CronLine.currentCronLine();
      if ((lastCronLine != null) && lastCronLine.matches(curCron)) {
	itIsTime = false;
      } else {
	lastCronLine = curCron;
	if (curCron.matches(cronLine)) {
	  itIsTime = true;
	}
      }
    }
    return itIsTime;
  }

  /**
   * Check if the harvest status has changed since the last check.
   * 
   * @return
   */
  public boolean statusChanged() {
    boolean changed;
    if (lastHarvestStatus == null) {
      changed = true;
    } else {
      changed = !(lastHarvestStatus == harvestJob.getStatus());
    }
    lastHarvestStatus = harvestJob.getStatus();
    return changed;
  }

  /**
   * Check if the harvest status (error) message has changed since the last
   * check.
   * 
   * @return
   */
  public boolean statusMsgChanged() {
    boolean changed;
    if (lastStatusMsg == null && harvestJob.getMessage() == null) {
      changed = false;
    } else if (lastStatusMsg == null) {
      changed = true;
    } else {
      changed = !(lastStatusMsg.equals(harvestJob.getMessage()));
    }
    lastStatusMsg = harvestJob.getMessage();
    return changed;
  }

  /**
   * Return last harvesting status.
   * 
   * @return harvesting status
   */
  public HarvestStatus getStatus() {
    return harvestJob.getStatus();
  }

  public boolean shallPersist() {
    if (harvestJob.isUpdated()) {
      harvestJob.clearUpdated();
      return true;
    }
    return false;
  }

}