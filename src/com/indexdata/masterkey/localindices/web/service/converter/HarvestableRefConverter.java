/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service.converter;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlAttribute;
//import javax.ws.rs.core.UriBuilder;

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
     * Creates a new instance of HarvestableRefConverter.
     *
     * @param entity associated entity
     * @param uri associated uri
     * @param isUriExtendable indicates whether the uri can be extended
     */
    public HarvestableRefConverter(Harvestable entity, URI uri, boolean isUriExtendable) {
        id = entity.getId();
        lastUpdated = entity.getLastUpdated();
        if (isUriExtendable) {
            try {
                this.uri = new URI(uri.toString() + entity.getId() + "/");
            } catch (URISyntaxException urie) {                
            }
            //this.uri = UriBuilder.fromUri(uri).path(entity.getId() + "/").build();
        }
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
