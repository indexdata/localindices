/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Corresponds to version 1 update
 * @author jakub
 */
@Entity
@NamedQueries({@NamedQuery(name = "Harvestable.findById", query = "SELECT o FROM Harvestable o WHERE o.id = :id")})
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public abstract class Harvestable implements Serializable, Cloneable {

    protected static final long serialVersionUID = 1L;
    // user-set properties
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;
    protected String name;
    //renamed v1
    protected String serviceProvider;
    //renamed v1
    protected String technicalNotes;
    //added v1
    protected String contactNotes;
    protected String scheduleString;
    protected Integer maxDbSize;
    protected Boolean enabled;
    @Temporal(TemporalType.TIMESTAMP)
    protected Date lastUpdated;
    //harvester-set properties
    //added v1
    @Temporal(TemporalType.TIMESTAMP)
    protected Date initiallyHarvested;
    @Temporal(TemporalType.TIMESTAMP)
    protected Date lastHarvestStarted;
    // added v1
    @Temporal(TemporalType.TIMESTAMP)
    protected Date lastHarvestFinished;
    protected String currentStatus;
    //renamed v1
    protected Integer amountHarvested;
    //renamed v1
    protected String message;

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

    public Integer getAmountHarvested() {
        return amountHarvested;
    }

    public void setAmountHarvested(Integer amountHarvested) {
        this.amountHarvested = amountHarvested;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // change that so that it check the reall class
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
}
