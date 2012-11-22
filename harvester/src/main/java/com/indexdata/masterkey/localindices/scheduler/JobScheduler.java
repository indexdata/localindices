/*
f * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.scheduler;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.dao.bean.HarvestablesDAOJPA;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;
import com.indexdata.masterkey.localindices.harvest.job.HarvestStatus;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorage;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorageFactory;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * JobScheduler schedules the job kept in the memory-based collection, keeps
 * them in touch with the persistent storage and reacts on status and error
 * changes.
 * 
 * @author jakub
 */
public class JobScheduler {

  private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
  private HarvestableDAO dao;
  private Map<Long, JobInstance> jobs = new HashMap<Long, JobInstance>();
  private Map<String, Object> config;

  public JobScheduler(Map<String, Object> config) {
    dao = new HarvestablesDAOJPA();
    this.config = config;
  }

  /**
   * Update the current job list to reflect updates in the persistent storage.
   */
  public void updateJobs() {
    Collection<HarvestableBrief> hbriefs = dao.retrieveBriefs(0,
	Integer.parseInt((String) config.get("harvester.max-jobs")));
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
	  // stop and signal to create new one
	  ji.stop();
	  jobs.remove(id);
	  ji = null;
	}
      }
      // no corresponding job in the list, create new one
      if (ji == null) {
	Harvestable harv = dao.retrieveFromBrief(hbrief);
	try {
	  HarvestStorage storage = selectHarvestStorage(harv);
	  ji = new JobInstance(harv, storage, (Proxy) config.get("harvester.http.proxy"),
	      hbrief.isEnabled());
	  jobs.put(id, ji);
	  logger.log(Level.INFO, "JOB#" + ji.getHarvestable().getId() + " created. Job Status: " + ji.getStatus());
	  if (!hbrief.isEnabled())
	    logger.log(Level.INFO, "JOB#" + ji.getHarvestable().getId() + " will be disabled.");
	  else {
	    if (HarvestStatus.valueOf(hbrief.getCurrentStatus()).equals(HarvestStatus.RUNNING)) {
	      logger.log(Level.ERROR, "JOB#" + ji.getHarvestable().getId() + " was logged as running, but no JobInstances was found.");
	      ji.start();
	      logger.log(Level.ERROR, "JOB#" + ji.getHarvestable().getId() + "  started");
	    }
	  }
	} catch (IllegalArgumentException e) {
	  logger.log(Level.ERROR, "Cannot update the current job list with " + harv.getId());
	  logger.log(Level.DEBUG, e);
	  continue;
	}
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

  private HarvestStorage selectHarvestStorage(Harvestable harv) {
    if (harv.getStorage() != null) {
      return HarvestStorageFactory.getStorage(harv);
    }
    logger.error("Not Storage configuration for Harvestable: " + harv.getName());
    return null;
  }

  /**
   * Start, report status and error of the scheduled jobs.
   */
  public void checkJobs() {
    for (JobInstance ji : jobs.values()) {
      switch (ji.getStatus()) {
      case FINISHED: // update the lastHarvestStarted (and harvestedUntil)
	// and send received signal
	ji.notifyFinish();
	logger.log(Level.INFO, "JOB#" + ji.getHarvestable().getId()
	    + " has finished. Persisting state...");
	dao.update(ji.getHarvestable());
	// persist from and until
	break;
      case ERROR:
      case NEW: // ask if time to run
      case OK:
	// should check harvested until?
	if (ji.timeToRun()) {
	  ji.start();
	}
	break;
      case SHUTDOWN:        // Status set on Servlet container shutdown, so the time can be way overdue
	if (ji.isEnabled()) // It could have disabled in the database. 
	  ji.start();
	break;
      case RUNNING: // do nothing (update progress bar?)
	break;
      case KILLED: // zombie thread
	break;
      }
      // TODO change the whole thing into an observer pattern
      boolean needsUpdate = false;
      if (ji.statusChanged()) {
	logger.log(Level.INFO,
	    "JOB#" + ji.getHarvestable().getId() + " status updated to " + ji.getStatus());
	ji.getHarvestable().setCurrentStatus(ji.getStatus().name());
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
      if (needsUpdate)
	dao.update(ji.getHarvestable());
    }
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
      jInfo.setStatus(ji.getStatus());
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
	logger.log(Level.INFO, "JOB#" + ji.getHarvestable().getId() + " status: " + ji.getStatus());
      if (ji.getStatus().equals(HarvestStatus.RUNNING)) {
	ji.getHarvestable().setCurrentStatus("" + HarvestStatus.SHUTDOWN);
	dao.update(ji.getHarvestable());
	logger.log(Level.INFO, "JOB#" + ji.getHarvestable().getId() 
	    		     + " status updated to " + ji.getHarvestable().getCurrentStatus() 
	    		     + " (Job Instance status: " + ji.getStatus() + ")");
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
    ji.stop();
  }
}
