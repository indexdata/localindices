/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.entity;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.indexdata.utils.CronLine;
import com.indexdata.utils.CronLineParseException;

/**
 * Corresponds to version 1 update
 *
 * @author Jakub
 * @author Dennis
 */
@Entity
@NamedQueries({ @NamedQuery(name = "Harvestable.findById", query = "SELECT o FROM Harvestable o WHERE o.id = :id") })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Harvestable implements Serializable, Cloneable, Filterable {

  protected static final long serialVersionUID = 1L;
  // user-set properties
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  protected Long id;
  protected String name;
  @Column(length = 4096)
  protected String description;
  // renamed v1
  protected String serviceProvider;
  protected String usedBy;
  protected String managedBy;
  // renamed v1
  @Column(length = 4096)
  protected String technicalNotes;
  // added v1
  @Column(length = 4096)
  protected String contactNotes;
  protected String scheduleString;
  protected Integer maxDbSize;
  protected Boolean enabled;
  @Temporal(TemporalType.TIMESTAMP)
  protected Date lastUpdated;
  // harvester-set properties
  // added v1
  @Temporal(TemporalType.TIMESTAMP)
  protected Date initiallyHarvested;
  @Temporal(TemporalType.TIMESTAMP)
  protected Date lastHarvestStarted;
  // added v1
  @Temporal(TemporalType.TIMESTAMP)
  protected Date lastHarvestFinished;
  protected String currentStatus;
  // renamed v1
  protected Long amountHarvested;
  // renamed v1
  @Column(length = 4096)
  protected String message;
  @Column(nullable = false)
  protected Boolean harvestImmediately;

  @ManyToOne(optional = true)
  protected Storage storage;

  @ManyToOne(optional = true)
  protected Transformation transformation;
  private Boolean overwrite;
  @Column(nullable=true)
  private String encoding;
  @Column(nullable=false)
  private Boolean allowErrors = false;
  @Column(nullable=false)
  private Integer timeout = 300; //SECS
  @Column(nullable=false)
  private Integer retryCount = 2;
  @Column(nullable=false)
  private Integer retryWait = 60; //SECS
  @Column(nullable=true)
  private Integer recordLimit = null;


  private String logLevel  = "INFO";
  private String failedRecordsLogging = "CLEAN_DIRECTORY";
  private Integer maxSavedFailedRecordsPerRun = 100;
  private Integer maxSavedFailedRecordsTotal = 1000;
  private String mailLevel = "WARN";
  private String mailAddress = null;
  private boolean diskRun = false;
  private boolean cacheEnabled = false;
  @Column(nullable=false)
  private boolean openAccess = false;
  @Column(nullable=true)
  private String originalUri = null;
  @Column(nullable=true)
  private String json = null;
  @Column(nullable=false)
  private boolean laxParsing = false;
  //moved from OaiPmhResource to facilitate manual increments
  @Temporal(TemporalType.TIMESTAMP)
  private Date fromDate;
  @Temporal(TemporalType.TIMESTAMP)
  private Date untilDate;
  @Column(nullable=true)
  private Integer storageBatchLimit = null;
  private String constantFields = null;
  @Column(nullable=false)
  private boolean storeOriginal = false;
  @Column(nullable=true)
  private String acl = null;

  @Transient
  public static List<String> KEYWORD_ALL_FIELDS = Arrays.asList(
                                    "name",
                                    "description",
                                    "technicalNotes",
                                    "contactNotes",
                                    "serviceProvider",
                                    "usedBy",
                                    "managedBy",
                                    "currentStatus");

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getCurrentStatus() {
    return currentStatus;
  }

  public void setCurrentStatus(String currentStatus) {
    this.currentStatus = currentStatus;
  }

  public String getTechnicalNotes() {
    return technicalNotes;
  }

  public void setTechnicalNotes(String technicalNotes) {
    this.technicalNotes = technicalNotes;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Date getLastHarvestStarted() {
    return lastHarvestStarted;
  }

  public void setLastHarvestStarted(Date lastHarvestStarted) {
    this.lastHarvestStarted = lastHarvestStarted;
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public Integer getMaxDbSize() {
    return maxDbSize;
  }

  public void setMaxDbSize(Integer maxDbSize) {
    this.maxDbSize = maxDbSize;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getScheduleString() {
    return scheduleString;
  }

  public void setScheduleString(String scheduleString) {
    this.scheduleString = scheduleString;
  }

  public String getServiceProvider() {
    return serviceProvider;
  }

  public void setServiceProvider(String serviceProvider) {
    this.serviceProvider = serviceProvider;
  }

  public String getUsedBy() {
    return usedBy;
  }

  public void setUsedBy(String usedBy) {
    this.usedBy = usedBy;
  }

  public String getManagedBy() {
    return managedBy;
  }

  public void setManagedBy(String managedBy) {
    this.managedBy = managedBy;
  }

  public Date getInitiallyHarvested() {
    return initiallyHarvested;
  }

  public void setInitiallyHarvested(Date initiallyHarvested) {
    this.initiallyHarvested = initiallyHarvested;
  }

  public String getContactNotes() {
    return contactNotes;
  }

  public void setContactNotes(String contactNotes) {
    this.contactNotes = contactNotes;
  }

  public Date getLastHarvestFinished() {
    return lastHarvestFinished;
  }

  public void setLastHarvestFinished(Date lastHarvestFinished) {
    this.lastHarvestFinished = lastHarvestFinished;
  }

  public Long getAmountHarvested() {
    return amountHarvested;
  }

  public void setAmountHarvested(Long amount) {
    this.amountHarvested = amount;
  }

  @Transient
  public Date getNextHarvestSchedule() {
    if (this.getScheduleString() != null) {

      try {
	return new CronLine(this.getScheduleString()).nextMatchingDate(new GregorianCalendar()
	    .getTime());
      } catch (CronLineParseException cronLineParseException) {
	// Error logged by CronLine, don't attempt to return a date to the UI.
	return null;
      } catch (NumberFormatException numberFormatExceptionException) {
	// Error logged by CronLine, don't attempt to return a date to the UI.
	return null;
      }
    }
    return null;
  }

  synchronized public void setHarvestImmediately(Boolean harvestImmediately) {
    this.harvestImmediately = harvestImmediately;
  }

  synchronized public Boolean getHarvestImmediately() {
    return harvestImmediately;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // change that so that it check the real class
    if (!(object instanceof Harvestable)) {
      return false;
    }
    Harvestable other = (Harvestable) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return this.getClass().getCanonicalName() + "[id=" + id + "]";
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  public Storage getStorage() {
    return storage;
  }

  public void setStorage(Storage storage) {
    this.storage = storage;
  }

  public Transformation getTransformation() {
    return transformation;
  }

  public void setTransformation(Transformation transformation) {
    this.transformation = transformation;
  }

  public void setOverwrite(Boolean overwrite) {
    this.overwrite = overwrite;
  }

  public Boolean getOverwrite() {
    if (overwrite != null)
      return overwrite;
    return false;
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String encoding) {
    //Force null on empty string
    if ("".equals(encoding))
      	encoding = null;
    this.encoding = encoding;
  }

  public Boolean getAllowErrors() {
    return allowErrors;
  }

  public void setAllowErrors(Boolean allowErrors) {
    this.allowErrors = allowErrors;
  }

  public Integer getTimeout() {
    return timeout;
  }

  public void setTimeout(Integer timeout) {
    this.timeout = timeout;
  }

  public Integer getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(Integer retryCount) {
    this.retryCount = retryCount;
  }

  public Integer getRetryWait() {
    return retryWait;
  }

  public void setRetryWait(Integer retryWait) {
    this.retryWait = retryWait;
  }

  public void reset() {
    setAmountHarvested(null);
    setInitiallyHarvested(null);
    setLastHarvestStarted(null);
    setLastHarvestFinished(null);
    setCurrentStatus("NEW");
  }

  public String getLogLevel() {
    return logLevel;
  }

  public void setLogLevel(String logLevel) {
    this.logLevel = logLevel;
  }

  public String getFailedRecordsLogging() {
    return failedRecordsLogging;
  }

  public void setFailedRecordsLogging(String failedRecordsLogging) {
    this.failedRecordsLogging = failedRecordsLogging;
  }

  public Integer getMaxSavedFailedRecordsPerRun() {
    return maxSavedFailedRecordsPerRun;
  }

  public void setMaxSavedFailedRecordsPerRun(Integer maxSavedFailedRecordsPerRun) {
    this.maxSavedFailedRecordsPerRun = maxSavedFailedRecordsPerRun;
  }

  public Integer getMaxSavedFailedRecordsTotal() {
    return maxSavedFailedRecordsTotal;
  }

  public void setMaxSavedFailedRecordsTotal(Integer maxSavedFailedRecordsTotal) {
    this.maxSavedFailedRecordsTotal = maxSavedFailedRecordsTotal;
  }

  public String getMailLevel() {
    return mailLevel;
  }

  public void setMailLevel(String mailLevel) {
    this.mailLevel = mailLevel;
  }

  public String getMailAddress() {
    return mailAddress;
  }

  public void setMailAddress(String mailAddresses) {
    this.mailAddress = mailAddresses;
  }

  public void setRecordLimit(Integer limit) {
    recordLimit = limit;
  }

  public Integer getRecordLimit() {
    return recordLimit;
  }

  public boolean isDiskRun() {
    return diskRun;
  }

  public void setDiskRun(boolean diskRun) {
    this.diskRun = diskRun;
  }

  public boolean isCacheEnabled() {
    return cacheEnabled;
  }

  public void setCacheEnabled(boolean cacheEnabled) {
    this.cacheEnabled = cacheEnabled;
  }

  public boolean isOpenAccess() {
    return openAccess;
  }

  public void setOpenAccess(boolean openAccess) {
    this.openAccess = openAccess;
  }

  public String getOriginalUri() {
    return originalUri;
  }

  public void setOriginalUri(String originalOri) {
    this.originalUri = originalOri;
  }

  public String getJson() {
    return json;
  }

  public void setJson(String json) {
    this.json = json;
  }

  public void setLaxParsing(boolean laxParsing) {
    this.laxParsing = laxParsing;
  }

  public boolean isLaxParsing() {
    return laxParsing;
  }

  public Date getFromDate() {
    return fromDate;
  }

  public void setFromDate(Date fromDate) {
    this.fromDate = fromDate;
  }

  public Date getUntilDate() {
    return untilDate;
  }

  public void setUntilDate(Date untilDate) {
    this.untilDate = untilDate;
  }

  public Integer getStorageBatchLimit() {
    return storageBatchLimit;
  }

  public void setStorageBatchLimit(Integer storageBatchLimit) {
    this.storageBatchLimit = storageBatchLimit;
  }

  public String getConstantFields() {
    return constantFields;
  }

  public void setConstantFields(String constantFields) {
    this.constantFields = constantFields;
  }

  public boolean isStoreOriginal() {
    return storeOriginal;
  }

  public void setStoreOriginal(boolean storeOriginal) {
    this.storeOriginal = storeOriginal;
  }

  public String getAcl() {
    return acl;
  }

  public void setAcl(String acl) {
    this.acl = acl;
  }

  @Transient
  public List<String> getKeywordAllFields () {
    return KEYWORD_ALL_FIELDS;
  }

}
