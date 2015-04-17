/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.job;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
      StringBuffer mailMessage = new StringBuffer("");
      List<Harvestable> harvestables = dao.retrieve(0,dao.getCount());
      Set<String> customers =getCustomers(harvestables);
      logger.info(getCustomers(harvestables).toString());
      logger.info(subject);
      for (String customer : customers) {
        mailMessage.append("<table><tr><td colspan='4'><h1>").append(customer).append("</h1></td></tr>");
        mailMessage.append("<tr><th width='33%'>Job</th><th>Status</th><th>Message</th><th>Amount</th></tr>");
        appendHarvestables(mailMessage, getHarvestablesByCustomer(harvestables, customer));
        mailMessage.append("</table>");
      }
      mailMessage.append("<table><tr><td colspan='4'><h1>Not assigned to customer</h1></td></tr>");
      mailMessage.append("<tr><th width='33%'>Job</th><th>Status</th><th>Message</th><th>Amount</th></tr>");
      appendHarvestables(mailMessage, getHarvestablesByCustomer(harvestables, null));
      mailMessage.append("</table>");
      msg = mailMessage.toString();
      setStatus(HarvestStatus.OK);
    } catch (Exception e) {
      if (isKillSent()) {
        String logMessage = "Status Job killed.";
        logger.log(Level.WARN, logMessage);
        setStatus(HarvestStatus.WARN, logMessage);
      }
      subject = "Status Job failed: " + e.getMessage();
      logger.log(Level.ERROR, subject , e);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      e.printStackTrace(new PrintStream(baos));
      try { 
        msg = baos.toString("UTF-8");
      } catch (UnsupportedEncodingException e1) { e1.printStackTrace(); }
    }
    finally {
      mailMessage(subject, msg);
      setStatus(HarvestStatus.FINISHED);
      shutdown();
    }
  }
  
  @Override
  protected String getMessageContentType() {
    return "text/html; charset=UTF-8";
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

  private Set<String> getCustomers (List<Harvestable> harvestables) {
    Set<String> customers = new TreeSet<String>();
    for (Harvestable harvestable : harvestables) {
      String customerField = harvestable.getCustomer();
      if (customerField != null && customerField.length()>0) {
        customerField = customerField.trim();
        customers.addAll(Arrays.asList(customerField.split("[ ]?,[ ]?")));
      }
    }
    return customers;
  }
  
  private void appendHarvestables(StringBuffer mailMessage, List<Harvestable> harvestables) {
    for (Harvestable harvestable : harvestables) {
      mailMessage.append("<tr><td  style='vertical-align:top;'>").append(harvestable.getName()).append(" ("+ harvestable.getId() +")")
      .append("</td><td style='vertical-align:top;'>")  
      .append(harvestable.getCurrentStatus()).append("</td><td style='vertical-align:top; font-style:italic; padding:2px 15px;'>")
      .append(harvestable.getMessage() != null ? harvestable.getMessage() : "No message").append("</td><td style='text-align:right; vertical-align:top;'>")
      .append(harvestable.getAmountHarvested() != null ? harvestable.getAmountHarvested() : 0).append("</td></tr>\n");

      logger.info(String.format("%8d \t %-45s \t %-8s \t %-10s \t %-8d",
          harvestable.getId(), harvestable.getName(), harvestable.getCurrentStatus(),
          (harvestable.getMessage() != null ? harvestable.getMessage() : "No message"),
          (harvestable.getAmountHarvested() != null ? harvestable.getAmountHarvested() : 0)));
    }
  }
  
  private List<Harvestable> getHarvestablesByCustomer (List<Harvestable> harvestables, String customer) {
    List<Harvestable> harvestablesFiltered = new ArrayList<Harvestable>();
    for (Harvestable harvestable : harvestables) {
      if (customer == null) {
        if (harvestable.getCustomer() == null || harvestable.getCustomer().trim().length()==0) {
          harvestablesFiltered.add(harvestable);
        }
      } else {
        if (harvestable.getCustomer() != null && harvestable.getCustomer().contains(customer)) {
          harvestablesFiltered.add(harvestable);
        }
      }
    }
    return harvestablesFiltered;
  }

}
