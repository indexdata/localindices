/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.localindexes.web.service.converter;

import com.indexdata.localindexes.web.entity.Harvestable;
import java.net.URI;
import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * 
 * @author jakub
 */
@XmlRootElement(name = "harvestableRef")
public class HarvestableRefConverter {

    private Long id;
    private URI uri;
    private Date lastUpdated;

    /** Creates a new instance of HarvestableRefConverter */
    public HarvestableRefConverter() {
    }
    
    /**
     * Creates a new instance of HarvestableRefConverter.
     *
     * @param entity associated entity
     */
    public HarvestableRefConverter(Harvestable entity) {
        id = entity.getId();
        lastUpdated = entity.getLastUpdated();
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
    
    @XmlElement
    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
