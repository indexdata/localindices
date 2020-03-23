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

import com.indexdata.masterkey.localindices.dao.EntityQuery;
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
      if (hasUsedByFilter() || hasManagedByFilter()) {
        mailMessage.append("Filtered by: ");
        if (hasUsedByFilter()) mailMessage.append(" Usage tags: " + statusJob.getUsedBy());
        if (hasManagedByFilter()) mailMessage.append(" Admin tags: " + statusJob.getManagedBy());
      }
      EntityQuery query = new EntityQuery();
      List<Harvestable> allHarvestables = dao.retrieve(0,dao.getCount(query), query);
      // Filter harvestables by criteria specified on the status job
      List<Harvestable> harvestables = applyStatusJobFilters(allHarvestables);

      // Write status matrices by usedBy tags and by contacts
      appendStats(mailMessage,harvestables);

      // Get all usedBy tags present on any harvestables 
      // (after filtering, if any) 
      Set<String> usedByTags = getUsedByTags(harvestables);

      // Write jobs list by usedBy tags (and filtered as specified).
      mailMessage.append("<h3>All jobs sorted by usage:</h3>");
      mailMessage.append("<table cellpadding='2px' border='1'>");
      mailMessage.append("<tr><th>Usage</th><th width='33%'>Job</th><th>Status</th><th>Message</th><th>Amount</th></tr>");
      for (String usedByTag : usedByTags) {
        appendHarvestables(mailMessage, getHarvestablesByUsedByTag(harvestables, usedByTag), usedByTag);
      }
      // Write jobs not tagged 
      appendHarvestables(mailMessage, getHarvestablesByUsedByTag(harvestables, null), "[No tag]");
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
      if  ((!hasManagedByFilter() || stringContainsItem(harvestable.getManagedBy(),getManagedByFilterTags(),true))
           && 
           (!hasUsedByFilter() || hasItemMatch(harvestable.getUsedBy(),getUsedByFilterTags(),true)))
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
   * Write status matrices by usage tags and by contact tags
   * 
   * @param message
   * @param harvestables
   */
  private void appendStats(StringBuffer message, List<Harvestable> harvestables) {
    StatsMatrix statusByUsage = new StatsMatrix();
    StatsMatrix statusByMngmt = new StatsMatrix();
    
    for (Harvestable harvestable : harvestables) {
      // Builds status-by-usage matrix
      String usedByCommaString = harvestable.getUsedBy();
      if (usedByCommaString != null && usedByCommaString.length()>0) {
        for (String usageTag : usedByCommaString.trim().split("[ ]?,[ ]?")) {
          if (hasUsedByFilter()) {
            if (hasItemMatch(getUsedByFilterTags(), Arrays.asList(usageTag),true)) {
              statusByUsage.addObservation(usageTag, harvestable.getCurrentStatus());
            }
          } else {
            statusByUsage.addObservation(usageTag, harvestable.getCurrentStatus());
          }
        }
      } else {
        statusByUsage.addObservation("[No tag]",harvestable.getCurrentStatus());
      }

      // Builds status-by-admin matrix
      String managedByCommaString = harvestable.getManagedBy();
      if (managedByCommaString != null && managedByCommaString.length()>0) {
        for (String mngmtTag : managedByCommaString.trim().split("[ ]?,[ ]?")) {
          if (hasManagedByFilter()) {
            if (hasItemMatch(getManagedByFilterTags(),Arrays.asList(mngmtTag),true)) {
              statusByMngmt.addObservation(mngmtTag, harvestable.getCurrentStatus());
            }
          } else {
            statusByMngmt.addObservation(mngmtTag, harvestable.getCurrentStatus());
          }
        }
      } else {
        statusByMngmt.addObservation("[No tag]",harvestable.getCurrentStatus());
      }
    }

    // Appends generated matrices to the message
    message.append("<table><tr><td colspan='2'><h3>Number of jobs by harvest-status</h3></td></tr>");
    message.append("<tr><td valign='top' width='50%'>");
    message.append("<table cellpadding='2px' border='1'>");
    message.append("<tr><th style='width:120px; text-align:left;'>By usage tags</th>");
    message.append("<th style='width:65px;'>Errors</th>")
    .append("<th style='width:65px;'>Warnings</th>")
    .append("<th style='width:65px;'>Okay</th>")
    .append("<th style='width:65px;'>New jobs</th>")
    .append("<th style='width:65px;'>Running</th>")
    .append("</tr>");
    for (String usageLabel : statusByUsage.getYLabels()) {
      message.append("<tr>");
      message.append("<td>"+usageLabel+"</td>")
      .append("<td align='right'>" + statusByUsage.getCount(usageLabel,"ERROR")+ "</td>")
      .append("<td align='right'>" + statusByUsage.getCount(usageLabel,"WARN") + "</td>")
      .append("<td align='right'>" + statusByUsage.getCount(usageLabel,"OK")   + "</td>")
      .append("<td align='right'>" + statusByUsage.getCount(usageLabel,"NEW")  + "</td>")
      .append("<td align='right'>" + statusByUsage.getCount(usageLabel,"RUNNING")  + "</td>");
      message.append("</tr>");
    }
    message.append("</table>");
    message.append("</td><td valign='top' width='50%'>");
    message.append("<table cellpadding='2px' border='1'>");
    message.append("<tr><th style='width:120px; text-align:left;'>By mngmt tags</th>");
    message.append("<th style='width:65px;'>Errors</th>")
           .append("<th style='width:65px;'>Warnings</th>")
           .append("<th style='width:65px;'>Okay</th>")
           .append("<th style='width:65px;'>New jobs</th>")
           .append("<th style='width:65px;'>Running</th>")
           .append("</tr>");
    for (String mgmtTag : statusByMngmt.getYLabels()) {
      message.append("<tr><td>").append(mgmtTag).append("</td>")
       .append("<td align='right'>" + statusByMngmt.getCount(mgmtTag,"ERROR")+ "</td>")
       .append("<td align='right'>" + statusByUsage.getCount(mgmtTag,"WARN") + "</td>")
       .append("<td align='right'>" + statusByMngmt.getCount(mgmtTag,"OK")   + "</td>")
       .append("<td align='right'>" + statusByMngmt.getCount(mgmtTag,"NEW")  + "</td>")
       .append("<td align='right'>" + statusByUsage.getCount(mgmtTag,"RUNNING")  + "</td>");
      message.append("</tr>");
    }
    message.append("</table>");
    message.append("</td></tr></table>");
  }

  /** 
   * Gets all usedBy tags occurring in any of the harvestables (except status jobs),
   * but filtered down to only those also present in the current status job's 
   * usage tag filter (if any). 
   *  
   * @param harvestables
   * @return
   */
  private Set<String> getUsedByTags (List<Harvestable> harvestables) {
    Set<String> usageTags = new TreeSet<String>();
    for (Harvestable harvestable : harvestables) {
      if (harvestable instanceof StatusResource) continue;
      String usedByCommaString = harvestable.getUsedBy();
      if (usedByCommaString != null && usedByCommaString.length()>0) {
        usedByCommaString = usedByCommaString.trim();
        List<String> harvestableUsageTags = Arrays.asList(usedByCommaString.split("[ ]?,[ ]?"));
        if (hasUsedByFilter()) {
          List<String> tagListsIntersect = new ArrayList<String>();
          for (String usedByTag : harvestableUsageTags) {
            if (hasItemMatch(usedByTag,getUsedByFilterTags(),true)) 
              tagListsIntersect.add(usedByTag);
          }
          usageTags.addAll(tagListsIntersect);
        } else {
          usageTags.addAll(harvestableUsageTags);
        }
      }
    }
    return usageTags;
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
   * Gets all harvestables (after overall filtering) that are tagged by 'usedBy'
   *  
   * @param harvestables
   * @param usedBy
   * @return
   */
  private List<Harvestable> getHarvestablesByUsedByTag (List<Harvestable> harvestables, String usageTag) {
    List<Harvestable> harvestablesFiltered = new ArrayList<Harvestable>();
    for (Harvestable harvestable : harvestables) {
      if (usageTag == null) {
        if (harvestable.getUsedBy() == null || harvestable.getUsedBy().trim().length()==0) {
          harvestablesFiltered.add(harvestable);
        }
      } else {
        if (harvestable.getUsedBy() != null && harvestable.getUsedBy().contains(usageTag)) {
          harvestablesFiltered.add(harvestable);
        }
      }
    }
    return harvestablesFiltered;
  }
  
  /**
   * Returns true if a usage tag filter was specified for this status job
   * @return
   */
  private boolean hasUsedByFilter() {
    return (statusJob.getUsedBy()!=null && statusJob.getUsedBy().trim().length()>0);
  }
  
  /**
   * Returns true if a contact tag filter was specified for this status job
   * @return
   */
  private boolean hasManagedByFilter() {
    return (statusJob.getManagedBy()!=null && statusJob.getManagedBy().trim().length()>0);
  }
  
  /**
   * Gets list of tags in specified contact filter for this status job
   * @return
   */
  private List<String> getManagedByFilterTags() {
    return Arrays.asList(statusJob.getManagedBy().trim().split("[ ]?,[ ]?")); 
  }
  
  /**
   * Gets list of tags in specified usage tag filter for this status job
   * @return
   */
  private List<String> getUsedByFilterTags() {
    return Arrays.asList(statusJob.getUsedBy().trim().split("[ ]?,[ ]?")); 
  }
  
  /**
   * Returns true if any element in list A is also in list B
   * 
   * @param lista
   * @param listb
   * @param ignoreCase
   * @return
   */
  private boolean hasItemMatch (List<String> lista, List<String> listb, boolean ignoreCase) {
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
  private boolean hasItemMatch (String commaSepStringA, String commaSepStringB, boolean ignoreCase) {
    List<String> listA = null;
    List<String> listB = null;
    if (commaSepStringA != null && commaSepStringA.length()>0) {
      listA = Arrays.asList(commaSepStringA.split("[ ]?,[ ]?"));
    }
    if (commaSepStringB != null && commaSepStringB.length()>0) {
      listB = Arrays.asList(commaSepStringB.split("[ ]?,[ ]?"));
    }
    return hasItemMatch(listA,listB,ignoreCase);
  }

  /**
   * Returns true if any element in comma separated A is also in list B.
   * 
   * @param commaSepStringA
   * @param listB
   * @param ignoreCase
   * @return
   */
  private boolean hasItemMatch (String commaSepStringA, List<String> listB, boolean ignoreCase) {
    List<String> listA = null;
    if (commaSepStringA != null && commaSepStringA.length()>0) {
      listA = Arrays.asList(commaSepStringA.split("[ ]?,[ ]?"));
    }
    return hasItemMatch(listA,listB,ignoreCase);
  }
  
  /**
   * Returns true if any to the strings in 'list' are found in 'string'
   * 
   * @param string
   * @param list
   * @param ignoreCase
   * @return
   */
  private boolean stringContainsItem(String string, List<String> list, boolean ignoreCase) {
    if (string != null && list !=null && list.size()>0) {
      for (String str : list) {
        if (ignoreCase) {
          if (string.toLowerCase().trim().contains(str.toLowerCase().trim()))
            return true;
        } else {
          if (string.trim().contains(str.trim()))
            return true;
        }
      }
    }
    return false;
  }
}
