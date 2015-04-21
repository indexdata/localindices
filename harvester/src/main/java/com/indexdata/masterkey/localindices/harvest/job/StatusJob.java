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
import com.indexdata.masterkey.localindices.notification.SenderFactory;
import com.indexdata.masterkey.localindices.util.StatsMatrix;
import com.indexdata.utils.TextUtils;

/**
 * This class handles a Harvest Connector Job
 * 
 * @author jakub
 */
public class StatusJob extends AbstractRecordHarvestJob {
  private String jobUrl = "";
  @SuppressWarnings("unused")
  private List<URL> urls = new ArrayList<URL>();
  private StatusResource statusJob;
  private Thread jobThread = null;
  private HarvestableDAO dao = new HarvestablesDAOJPA();
  
  public StatusJob(StatusResource resource, Proxy proxy) {
    this.statusJob = resource;
    setStatus(HarvestStatus.valueOf(resource.getCurrentStatus()));
    this.statusJob.setMessage(null);
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
    String adminUrl = TextUtils.joinPath(SenderFactory.getSender().getAdminUrl());
    jobUrl = TextUtils.joinPath(adminUrl, "jobs", "log.xhtml");
    String subject = "Job Status Report";
    String msg     = "";
    statusJob.setMailLevel("OK");
    setStatus(HarvestStatus.RUNNING);
    try {
      StringBuffer mailMessage = new StringBuffer("");
      // Info on the filtering applied for this job
      if (hasCustomerFilter() || hasContactFilter()) {
        mailMessage.append("Filtered by: ");
        if (hasCustomerFilter()) mailMessage.append(" Customer(s): " + statusJob.getCustomer());
        if (hasContactFilter()) mailMessage.append(" Contact(s): " + statusJob.getContactNotes());
      }
      List<Harvestable> allHarvestables = dao.retrieve(0,dao.getCount());
      // Filter harvestables by criteria specified on the status job
      List<Harvestable> harvestables = applyStatusJobFilters(allHarvestables);

      // Write status matrices by customer and by contacts
      appendStats(mailMessage,harvestables);

      // Get all customer tags present on any harvestables 
      // (after filtering, if any) 
      Set<String> customers = getCustomers(harvestables);

      // Write jobs list by customer (and filtered as specified).
      mailMessage.append("<h3>All jobs sorted by customer:</h3>");
      mailMessage.append("<table>");
      mailMessage.append("<tr><th>Customer</th><th width='33%'>Job</th><th>Status</th><th>Message</th><th>Amount</th></tr>");
      for (String customer : customers) {
        appendHarvestables(mailMessage, getHarvestablesByCustomer(harvestables, customer), customer);
      }
      // Write jobs not tagged with any customer
      appendHarvestables(mailMessage, getHarvestablesByCustomer(harvestables, null), "[No cust.]");
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

  /**
   * Returns a list of harvestables (excluding status job) filtered by 
   * the criteria specified by this status job. 
   * 
   * @param harvestables
   * @return
   */
  private List<Harvestable> applyStatusJobFilters (List<Harvestable> harvestables) {
    List<Harvestable> filteredList = new ArrayList<Harvestable>();
    for (Harvestable harvestable : harvestables) {
      // Skip this status job itself in report
      if (harvestable instanceof StatusResource) 
        continue;
      if  ((!hasContactFilter() || contains(harvestable.getContactNotes(),getContactFilterTags(),true))
           && 
           (!hasCustomerFilter() || overlaps(harvestable.getCustomer(),getCustomerFilterTags(),true)))
        filteredList.add(harvestable);
    }
    return filteredList;
  }

  @Override
  protected String getMessageContentType() {
    return "text/html; charset=UTF-8";
  }

  @Override
  public Harvestable getHarvestable() {
    return statusJob;
  }
 
  public Thread getJobThread() {
    return jobThread;
  }

  public void setJobThread(Thread jobThread) {
    this.jobThread = jobThread;
  }
  
  /**
   * Write status matrices by customer tags and by contact tags
   * 
   * @param message
   * @param harvestables
   */
  private void appendStats(StringBuffer message, List<Harvestable> harvestables) {
    StatsMatrix statusByCustomer = new StatsMatrix();
    StatsMatrix statusByContact = new StatsMatrix();
    
    for (Harvestable harvestable : harvestables) {
      // Builds customer-by-status matrix
      String customerField = harvestable.getCustomer();
      if (customerField != null && customerField.length()>0) {
        for (String customer : customerField.trim().split("[ ]?,[ ]?")) {
          if (hasCustomerFilter()) {
            if (getCustomerFilterTags().contains(customer)) {
              statusByCustomer.addObservation(customer, harvestable.getCurrentStatus());
            }
          } else {
            statusByCustomer.addObservation(customer, harvestable.getCurrentStatus());
          }
        }
      } else {
        statusByCustomer.addObservation("[No cust.]",harvestable.getCurrentStatus());
      }
      // Builds contactNotes-by-status matrix
      String contactNotes = harvestable.getContactNotes();
      if (contactNotes != null && contactNotes.length()>0) {
        statusByContact.addObservation(contactNotes.substring(0, Math.min(70, contactNotes.length()-1)).trim(), harvestable.getCurrentStatus());
      } else {
        statusByContact.addObservation("[No contact]",harvestable.getCurrentStatus());
      }
    }

    // Appends generated matrices to the message
    message.append("<h3>Number of jobs by harvest-status</h3>");
    message.append("<table cellspacing='10px'>");
    message.append("<tr><th style='width:120px; text-align:left;'>By customer</th><th style='width:70px;'>Errors</th><th style='width:70px;'>Okay</th><th style='width:70px;'>New jobs</th></tr>");
    for (String customer : statusByCustomer.getYLabels()) {
      message.append("<tr>");
      message.append("<td>"+customer+"</td>")
      .append("<td align='right'>" + statusByCustomer.getCount(customer,"ERROR")+ "</td>")
      .append("<td align='right'>" + statusByCustomer.getCount(customer,"OK")   + "</td>")
      .append("<td align='right'>" + statusByCustomer.getCount(customer,"NEW")  + "</td>");
      message.append("</tr>");
    }
    message.append("</table>");

    message.append("<table cellspacing='10px'>");
    message.append("<tr><th style='width:120px; text-align:left;'>By contact</th><th style='width:70px;'>Errors</th><th style='width:70px;'>Okay</th><th style='width:70px;'>New jobs</th></tr>");
    for (String contact : statusByContact.getYLabels()) {
      message.append("<tr><td>").append(contact).append("</td>")
       .append("<td align='right'>" + statusByContact.getCount(contact,"ERROR")+ "</td>")
       .append("<td align='right'>" + statusByContact.getCount(contact,"OK")   + "</td>")
       .append("<td align='right'>" + statusByContact.getCount(contact,"NEW")  + "</td>");
      message.append("</tr>");
    }
    message.append("</table>");
  }

  /** 
   * Gets all customer tags occurring in any of the harvestables (except status jobs),
   * but filtered down to only those also present in the current status job's 
   * customer tag filter (if any). 
   *  
   * @param harvestables
   * @return
   */
  private Set<String> getCustomers (List<Harvestable> harvestables) {
    Set<String> customers = new TreeSet<String>();
    for (Harvestable harvestable : harvestables) {
      if (harvestable instanceof StatusResource) continue;
      String customerField = harvestable.getCustomer();
      if (customerField != null && customerField.length()>0) {
        customerField = customerField.trim();
        List<String> harvestablesCustomerTags = Arrays.asList(customerField.split("[ ]?,[ ]?"));
        if (hasCustomerFilter()) {
          List<String> customersIntersect = new ArrayList<String>();
          for (String customerTag : harvestablesCustomerTags) {
            if (overlaps(customerTag,getCustomerFilterTags(),true)) 
              customersIntersect.add(customerTag);
          }
          customers.addAll(customersIntersect);
        } else {
          customers.addAll(harvestablesCustomerTags);
        }
      }
    }
    return customers;
  }

  /**
   * Writes list of the provided harvestables
   * 
   * @param mailMessage
   * @param harvestables
   * @param label
   */
  private void appendHarvestables(StringBuffer mailMessage, List<Harvestable> harvestables, String label) {
    for (Harvestable harvestable : harvestables) {
      mailMessage.append("<tr><td valign=\"top\" style='width:80px;'>"+label+"</td><td style='vertical-align:top;'>")
      .append(harvestable.getName())
      .append(" (<a href=\""+jobUrl+"?resourceId="+harvestable.getId() + "#bottom\">")
      .append(harvestable.getId())
      .append("</a>)")
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
  
  /**
   * Gets all harvestables (after overall filtering) that are tagged by 'customer'
   *  
   * @param harvestables
   * @param customer
   * @return
   */
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
  
  /**
   * Returns true if a customer filter was specified for this status job
   * @return
   */
  private boolean hasCustomerFilter() {
    return (statusJob.getCustomer()!=null & statusJob.getCustomer().trim().length()>0);
  }
  
  /**
   * Returns true if a contact tag filter was specified for this status job
   * @return
   */
  private boolean hasContactFilter() {
    return (statusJob.getContactNotes()!=null & statusJob.getContactNotes().trim().length()>0);
  }
  
  /**
   * Gets list of tags in specified contact filter for this status job
   * @return
   */
  private List<String> getContactFilterTags() {
    return Arrays.asList(statusJob.getContactNotes().trim().split("[ ]?,[ ]?")); 
  }
  
  /**
   * Gets list of tags in specified customer filter for this status job
   * @return
   */
  private List<String> getCustomerFilterTags() {
    return Arrays.asList(statusJob.getCustomer().trim().split("[ ]?,[ ]?")); 
  }
  
  /**
   * Returns true if any element in list A is also in list B
   * 
   * @param lista
   * @param listb
   * @param ignoreCase
   * @return
   */
  private boolean overlaps (List<String> lista, List<String> listb, boolean ignoreCase) {
    if (lista != null && lista.size()>0 && listb!=null && listb.size()>0) 
      for (String itema : lista) 
        for (String itemb : listb) 
          if (ignoreCase)
            if (itema.trim().equalsIgnoreCase(itemb.trim())) 
              return true;
          else
            if (itema.trim().equals(itemb.trim()))
              return true;
    return false;
  }
  
  /**
   * Returns true if any element in comma separated A is also in 
   * comma separated B.
   * 
   * @param commaSepStringA
   * @param commaSepStringB
   * @param ignoreCase
   * @return
   */
  @SuppressWarnings("unused")
  private boolean overlaps (String commaSepStringA, String commaSepStringB, boolean ignoreCase) {
    List<String> listA = null;
    List<String> listB = null;
    if (commaSepStringA != null && commaSepStringA.length()>0) {
      listA = Arrays.asList(commaSepStringA.split("[ ]?,[ ]?"));
    }
    if (commaSepStringB != null && commaSepStringB.length()>0) {
      listB = Arrays.asList(commaSepStringB.split("[ ]?,[ ]?"));
    }
    return overlaps(listA,listB,ignoreCase);
  }

  /**
   * Returns true if any element in comma separated A is also in list B.
   * 
   * @param commaSepStringA
   * @param listB
   * @param ignoreCase
   * @return
   */
  private boolean overlaps (String commaSepStringA, List<String> listB, boolean ignoreCase) {
    List<String> listA = null;
    if (commaSepStringA != null && commaSepStringA.length()>0) {
      listA = Arrays.asList(commaSepStringA.split("[ ]?,[ ]?"));
    }
    return overlaps(listA,listB,ignoreCase);
  }
  
  /**
   * Returns true if any to the strings in 'list' are found in 'string'
   * 
   * @param string
   * @param list
   * @param ignoreCase
   * @return
   */
  private boolean contains(String string, List<String> list, boolean ignoreCase) {
    for (String str : list) {
      if (ignoreCase) {
        if (string.toLowerCase().trim().contains(str.toLowerCase().trim()))
          return true;
      } else {
        if (string.trim().contains(str.trim()))
          return true;
      }
    }
    return false;
  }

}
