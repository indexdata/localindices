/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.job;

import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.dao.bean.HarvestablesDAOJPA;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.StatusResource;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;

/**
 * This class handles a Harvest Connector Job
 * 
 * @author jakub
 */
public class StatusJob extends AbstractRecordHarvestJob {
  @SuppressWarnings("unused")
  private List<URL> urls = new ArrayList<URL>();
  private StatusResource resource;
  private Thread jobThread = null;
  private HarvestableDAO dao = new HarvestablesDAOJPA();
  
  public StatusJob(StatusResource resource, Proxy proxy) {
    this.resource = resource;
    setStatus(HarvestStatus.valueOf(resource.getCurrentStatus()));
    this.resource.setMessage(null);
    logger = new FileStorageJobLogger(this.getClass(), resource);
  }

  public String getMessage() {
    return error;
  }

  @Override
  public synchronized void kill() {
    super.kill();
    // Requires that the job instances configures the thread
    if (jobThread != null) {
      jobThread.interrupt();
    }
  }

  public void run() {
    String subject = "Job Status Report";
    String msg     = "";
    resource.setMailLevel("OK");
    setStatus(HarvestStatus.RUNNING);
    try {
      
      StringBuffer mailMesssage = new StringBuffer("Status of Jobs:\n");
      List<HarvestableBrief> harvestables = dao.retrieveBriefs(0,dao.getCount());
      logger.info(subject);
      for (HarvestableBrief brief : harvestables) {
	String logLine = String.format("%10d %-50s %-10s %10d", brief.getId(), brief.getName(), brief.getCurrentStatus(), (brief.getAmountHarvested() != null ? brief.getAmountHarvested() : -1));
	logger.info(logLine);
	mailMesssage.append(logLine).append("\n");
	setStatus(HarvestStatus.OK);
      }
      msg = mailMesssage.toString();
    } catch (Exception e) {
      if (isKillSent()) {
	String logMessage = "Status Job killed.";
 	logger.log(Level.WARN, logMessage);
	setStatus(HarvestStatus.WARN, logMessage);
      }
      subject = "Status Job failed: " + e.getMessage();
      logger.log(Level.ERROR, subject , e);
      msg = e.getStackTrace().toString();
    }
    finally {
      mailMessage(subject, msg);
      setStatus(HarvestStatus.FINISHED);
      
      shutdown();
    }
  }

  @Override
  public Harvestable getHarvestable() {
    return resource;
  }
 
  public Thread getJobThread() {
    return jobThread;
  }

  public void setJobThread(Thread jobThread) {
    this.jobThread = jobThread;
  }

}
