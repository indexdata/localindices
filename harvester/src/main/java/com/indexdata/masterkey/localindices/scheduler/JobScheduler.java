/*
f * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.scheduler;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.dao.bean.HarvestablesDAOJPA;
import com.indexdata.masterkey.localindices.dao.bean.StoragesDAOJPA;
import com.indexdata.masterkey.localindices.dao.StorageDAO;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.job.HarvestStatus;
import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;

/**
 * JobScheduler schedules the job kept in the memory-based collection, keeps
 * them in touch with the persistent storage and reacts on status and error
 * changes.
 * 
 * @author jakub
 */
public class JobScheduler implements JobNotifications {

  private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
  private HarvestableDAO harvestableDao;
  private StorageDAO storageDao; 
  private Map<Long, JobInstance> jobs = new HashMap<Long, JobInstance>();
  private Map<String, Object> config;
  @SuppressWarnings("unused")
  private Properties props;
  private JobNotifications notifications;
  
  public JobScheduler(Map<String, Object> config, Properties props) {
    harvestableDao = new HarvestablesDAOJPA();
    storageDao = new StoragesDAOJPA();

    this.config = config;
    this.props = props;
    notifications = this;
  }
  
  /**
   * Update the current job list to reflect updates in the persistent storage.
   */
  public void updateJobs() {
    Collection<HarvestableBrief> hbriefs = harvestableDao.retrieveBriefs(0, harvestableDao.getCount());
    if (hbriefs == null) {
      logger.log(Level.ERROR, "Cannot update harvesting jobs, retrieved list is empty.");
      return;
    }
    // mark all job so we know what to remove
    for (JobInstance j : jobs.values()) {
      j.seen = false;
    }
    for (HarvestableBrief hbrief : hbriefs) {
      Long id = hbrief.getId();
      JobInstance ji = jobs.get(id);
      // corresponding job is in the current list and is enabled
      if (ji != null) {
	// has been re-configured (also dis-/enabled)
	if (!ji.getHarvestable().getLastUpdated().equals(hbrief.getLastUpdated())) {
	  logger.log(Level.INFO, "JOB#" + ji.getHarvestable().getId() + " parameters changed (LU "
	      + hbrief.getLastUpdated() + "), stopping thread and destroying job");
	  // Remove the job now should ensure that we don't override the updated job  
  	  jobs.remove(ji.getHarvestable().getId());
  	  ji.stop();
	}
      }
      // no corresponding job in the list, create new one
      if (ji == null) {
	Harvestable harv = harvestableDao.retrieveFromBrief(hbrief);
	ji = addJob(harv);
      }
      ji.seen = true;
    }

    // kill jobs with no entities in the WS
    for (Iterator<JobInstance> it = jobs.values().iterator(); it.hasNext();) {
      JobInstance ji = it.next();
      if (!ji.seen) {
	logger.log(Level.INFO, "JOB#" + ji.getHarvestable().getId()
	    + " no longer in the DB. Stopping the job.");
	ji.stop();
	it.remove();
      }
    }
  }
  /**
   * Start, report status and error of the scheduled jobs.
   */
  public void checkJobs() {
    for (JobInstance ji : jobs.values()) {
	if (!ji.isRunning() && ji.timeToRun()) {
	  ji.start();
	}
	boolean needsUpdate = checkUpdate(ji);
	if (needsUpdate)
	  harvestableDao.update(ji.getHarvestable());
    }
  }
  
  public int doCmd(Harvestable harvestable, String cmd) {
    if (cmd.equals("stop")) {
      Long id = harvestable.getId();
      JobInstance ji = jobs.get(id); 
      if (ji != null) {
	  ji.stop();
      }
      else 
	logger.warn("Nothing to stop. Job " + id  + " is not running");
    }
    if (cmd.equals("run")) {
      Long id = harvestable.getId();
      JobInstance ji = jobs.get(id); 
      if (ji != null) {
	  ji.start();
      }
      else 
	logger.warn("Nothing to run. Job " + id  + " is not present");
    }
    return 0; 
  }

  protected boolean checkUpdate(JobInstance ji) {
    // TODO change the whole thing into an observer pattern
    boolean needsUpdate = false;
    if (ji.statusChanged()) {
    logger.log(Level.INFO,
        "JOB#" + ji.getHarvestable().getId() + " status updated to " + ji.getJobStatus());
    ji.getHarvestable().setCurrentStatus(ji.getJobStatus().name());
    needsUpdate = true;
    }
    if (ji.statusMsgChanged()) {
    logger.log(Level.INFO, "JOB#" + ji.getHarvestable().getId()
        + " - status message updated - " + ji.getHarvestable().getMessage());
    needsUpdate = true;
    }
    if (ji.shallPersist()) {
    needsUpdate = true;
    }
    return needsUpdate;
  }

  /**
   * Return a collection of status and config information on the scheduled jobs.
   * 
   * @return collection of JobInfo objects
   */
  public Collection<JobInfo> getJobInfo() {
    Collection<JobInfo> jInfoList = new ArrayList<JobInfo>();
    for (JobInstance ji : jobs.values()) {
      JobInfo jInfo = new JobInfo();
      jInfo.setHarvestable(ji.getHarvestable());
      jInfo.setStatus(ji.getJobStatus());
      jInfo.setError(ji.getHarvestable().getMessage());
      jInfo.setHarvestPeriod("");
      jInfoList.add(jInfo);
    }
    return jInfoList;
  }

  /**
   * Brutally stop all jobs.
   */
  public void stopAllJobs() {
    logger.log(Level.INFO, "StopAllJobs");
    for (JobInstance ji : jobs.values()) {
	logger.log(Level.INFO, "JOB#" + ji.getHarvestable().getId() + " status: " + ji.getJobStatus());
      if (ji.getJobStatus().equals(HarvestStatus.RUNNING)) {
	ji.getHarvestable().setCurrentStatus("" + HarvestStatus.SHUTDOWN);
	harvestableDao.update(ji.getHarvestable());
	ji.stop();
	logger.log(Level.INFO, "JOB#" + ji.getHarvestable().getId() 
	    		     + " status updated to " + ji.getHarvestable().getCurrentStatus() 
	    		     + " (Job Instance status: " + ji.getJobStatus() + ")");
      }
      ji.stop();
    }
  }
  
  /**
   * Stop the job with given id.
   * 
   * @param jobId
   */
  public void stopJob(Long jobId) {
    JobInstance ji = jobs.get(jobId);
    if (ji != null) {
      ji.stop();
      jobs.remove(ji);
    }
  }
  
  public JobInstance addJob(Harvestable harvestable) {
    Long id = harvestable.getId();
    try {
      JobInstance ji = new JobInstance(harvestable, 
	  	(Proxy) config.get("harvester.http.proxy"),
	  	harvestable.getEnabled(), notifications, storageDao);
      jobs.put(id, ji);
      ji.start();
      logger.log(Level.INFO, "JOB#" + ji.getHarvestable().getId() + " started.");
      return ji;
    } catch (IllegalArgumentException e) {
      logger.log(Level.ERROR, "Cannot update the current job list with " + harvestable.getId());
      logger.log(Level.DEBUG, e);
    }
    return null;
  }

  protected JobInstance validate(RecordHarvestJob job) {
    Harvestable harvestable = job.getHarvestable();
    Long id = job.getHarvestable().getId();
    JobInstance ji = jobs.get(id);
    if (ji == null) {
      logger.warn("finsish: Job missing: " + job);
      return ji;
    }
    if (harvestable != ji.getHarvestable()) {
      logger.error("Different harvestables: " + harvestable + "!=" + job.getHarvestable());
      // Return null to ensure we don't override. 
    }
    return ji;
  }

  @Override
  public void finished(RecordHarvestJob job) {
    JobInstance ji = validate(job);
    if (ji != null) {
      ji.notifyFinish();
    }
    Harvestable harvestable = job.getHarvestable();
    jobs.remove(harvestable.getId());
    logger.log(Level.INFO, "JOB(" + harvestable + ") has finished. Persisting state...");
    harvestableDao.update(harvestable);
  }

  @Override
  public void aborted(RecordHarvestJob job) {
    JobInstance ji = validate(job);
    logger.log(Level.INFO, "JOB(" + ji.getHarvestable() + ") has aborted.");
    // Do not persist or persist abort status?  
    jobs.remove(ji.getHarvestable().getId());
  }

  @Override
  public void persist(RecordHarvestJob job) {
    harvestableDao.update(job.getHarvestable());
  }

  public HarvestableDAO getHarvestableDao() {
    return harvestableDao;
  }

  public void setHarvestableDao(HarvestableDAO harvestableDao) {
    this.harvestableDao = harvestableDao;
  }

  public StorageDAO getStorageDao() {
    return storageDao;
  }

  public void setStorageDao(StorageDAO storageDao) {
    this.storageDao = storageDao;
  }

  public void setNotifications(JobNotifications notifications) {
    this.notifications = notifications;
  }
}
