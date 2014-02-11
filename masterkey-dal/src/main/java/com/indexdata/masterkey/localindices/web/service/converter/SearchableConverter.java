/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service.converter;

import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import com.indexdata.masterkey.localindices.entity.Harvestable;


/**
 *
 * @author jakub
 */

@XmlRootElement(name="record")
public class SearchableConverter {
    private Harvestable entity;
    private URI uri;
    
    /** Creates a new instance of HarvestableConverter */
    public SearchableConverter() {
    }
    
    /**
     * Creates a new instance of HarvestableConverter.
     *
     * @param entity associated entity
     */
    public SearchableConverter(Harvestable entity) {
        this.entity = entity;
    }

    /**
     * Creates a new instance of HarvestableConverter.
     *
     * @param entity associated entity
     * @param uri associated uri
     */
    public SearchableConverter(Harvestable entity, URI uri) {
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
