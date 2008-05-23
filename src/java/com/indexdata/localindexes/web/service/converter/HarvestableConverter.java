/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.localindexes.web.service.converter;

import com.indexdata.localindexes.web.entity.*;
import java.net.URI;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;


/**
 *
 * @author jakub
 */

@XmlRootElement(name = "harvestable")
public class HarvestableConverter {
    private Harvestable entity;
    private URI uri;
    
    /** Creates a new instance of HarvestableConverter */
    public HarvestableConverter() {
    }
    
    /**
     * Creates a new instance of HarvestableConverter.
     *
     * @param entity associated entity
     */
    public HarvestableConverter(Harvestable entity) {
        this.entity = entity;
    }

    /**
     * Creates a new instance of HarvestableConverter.
     *
     * @param entity associated entity
     * @param uri associated uri
     */
    public HarvestableConverter(Harvestable entity, URI uri) {
        this.entity = entity;
        this.uri = uri;
    }

    /**
     * Getter for id.
     *
     * @return value for id
     */
    @XmlElement
    public Long getId() {
        return entity.getId();
    }

    /**
     * Setter for id.
     *
     * @param value the value to set
     */
    public void setId(Long value) {
        entity.setId(value);
    }

    /**
     * Returns the URI associated with this converter.
     *
     * @return the uri
     */
    @XmlAttribute(name = "uri")
    public URI getResourceUri() {
        return uri;
    }
    
    public void setResourceUri(URI uri) {
        this.uri = uri;
    }

    /**
     * Returns the Harvestable entity.
     *
     * @return an entity
     */
    @XmlElementRef
    public Harvestable getEntity() {
        return entity;
    }

    /**
     * Sets the Harvestable entity.
     *
     * @param entity to set
     */
    public void setEntity(Harvestable entity) {
        this.entity = entity;
    }
}
