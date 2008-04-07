/*
 *  HarvestableConverter
 *
 * Created on April 4, 2008, 12:06 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.indexdata.localindexes.web.converter;

import com.indexdata.localindexes.web.entitybeans.Harvestable;
import java.net.URI;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlAttribute;


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
        entity = new Harvestable();
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

    /**
     * Returns the Harvestable entity.
     *
     * @return an entity
     */
    //@XmlTransient
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
