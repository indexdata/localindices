/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service.converter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.indexdata.masterkey.localindices.entity.Harvestable;

/**
 * 
 * @author jakub
 */
@XmlRootElement(name = "harvestableBrief")
public class HarvestableBrief implements Comparable<Object> {
    private Long id;
    private URI uri;
    private String name;
    private String currentStatus;
    private String message;
    private Date lastHarvestStarted;
    private Date lastHarvestFinished;
    private Date nextHarvestSchedule;
    private boolean enabled;
    private Date lastUpdated;

    /** Creates a new instance of HarvestableBrief */
    public HarvestableBrief() {
    }
    
    /**
     * Creates a new instance of HarvestableBrief.
     *
     * @param entity associated entity
     */
    public HarvestableBrief(Harvestable entity) {
        id = entity.getId();
        name = entity.getName();
        currentStatus = entity.getCurrentStatus();
        message = entity.getMessage();
        lastHarvestStarted = entity.getLastHarvestStarted();
        lastHarvestFinished = entity.getLastHarvestFinished();
        nextHarvestSchedule = entity.getNextHarvestSchedule();
        enabled = entity.getEnabled();
        lastUpdated = entity.getLastUpdated();
    }

    /**
     * Creates a new instance of HarvestableBrief.
     *
     * @param entity associated entity
     * @param uri associated uri
     * @param isUriExtendable indicates whether the uri can be extended
     */
    public HarvestableBrief(Harvestable entity, URI uri, boolean isUriExtendable) {
        this(entity);
        if (isUriExtendable) {
            try {
                this.uri = new URI(uri.toString() + entity.getId() + "/");
            } catch (URISyntaxException urie) {              
            }
        }
    }
    
    /**
     * Returns the URI associated with this reference converter.
     *
     * @return the converted uri
     */
    @XmlAttribute(name = "uri")
    
    public URI getResourceUri() {
        return uri;
    }

    /**
     * Sets the URI for this reference converter.
     *
     * @param uri resource uri
     */
    public void setResourceUri(URI uri) {
        this.uri = uri;
    }

    /**
     * Getter for id.
     *
     * @return value for id
     */
    @XmlElement
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    @XmlElement
    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @XmlElement(name="enabled")
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @XmlElement
    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    @XmlElement
    public Date getLastHarvestFinished() {
        return lastHarvestFinished;
    }

    public void setLastHarvestFinished(Date lastHarvestFinished) {
        this.lastHarvestFinished = lastHarvestFinished;
    }

    @XmlElement
    public Date getLastHarvestStarted() {
        return lastHarvestStarted;
    }

    public void setLastHarvestStarted(Date lastHarvestStarted) {
        this.lastHarvestStarted = lastHarvestStarted;
    }

    @XmlElement
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @XmlElement
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement
    public Date getNextHarvestSchedule() {
        return nextHarvestSchedule;
    }

    public void setNextHarvestSchedule(Date nextHarvestSchedule) {
        this.nextHarvestSchedule = nextHarvestSchedule;
    }
    
    public int compareTo (Object brief) {        
        return this.getName().compareTo(((HarvestableBrief)brief).getName());
    }
    
    public boolean equals(Object brief) {
        if (brief instanceof HarvestableBrief) {
            return (this.getName().equals(((HarvestableBrief)brief).getName()));
        } else {
            return false;
        }             
    }
    
    public int hashCode() {
        return this.getName().hashCode();
    }
    
}
