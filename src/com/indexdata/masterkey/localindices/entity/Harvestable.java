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
 *
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
    protected String title;
    protected String description;
    protected String scheduleString;
    protected Integer maxDbSize;
    protected Boolean enabled;
    @Temporal(TemporalType.TIMESTAMP)
    protected Date lastUpdated;
    //harvester-set properties
    @Temporal(TemporalType.TIMESTAMP)
    protected Date lastHarvestStarted;
    protected String currentStatus;
    protected Integer recordsHarvested;
    // boolean immediately?

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Integer getRecordsHarvested() {
        return recordsHarvested;
    }

    public void setRecordsHarvested(Integer recordsHarvested) {
        this.recordsHarvested = recordsHarvested;
    }

    public String getScheduleString() {
        return scheduleString;
    }

    public void setScheduleString(String scheduleString) {
        this.scheduleString = scheduleString;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
