/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.indexdata.localindexes.web.entitybeans;

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
//@XmlRootElement(name="abstractHarvestable")
public abstract class Harvestable implements Serializable {
    protected static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;
    protected String name;
    protected String title;
    protected String description;
    @Temporal(TemporalType.TIMESTAMP)
    protected Date lastUpdated;
    //@Temporal(TemporalType.TIMESTAMP)
    //protected Date lastHarvestStarted;
    protected Boolean active; // rename to enabled
    protected String currentStatus; // error string
    protected Integer recordsHarvested;
    protected Integer maxDbSize;
    // String cronString
    // Boolean immediately

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

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
        // TODO: Warning - this method won't work in the case the id fields are not set
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
        return "com.indexdata.localindexes.web.entitybeans.Harvestable[id=" + id + "]";
    }

}
