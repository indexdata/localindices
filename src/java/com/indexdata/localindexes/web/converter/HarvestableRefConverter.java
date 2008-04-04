/*
 *  HarvestableRefConverter
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
import javax.ws.rs.core.UriBuilder;


/**
 *
 * @author jakub
 */

@XmlRootElement(name = "harvestableRef")
public class HarvestableRefConverter {
    private Harvestable entity;
    private boolean isUriExtendable;
    private URI uri;
    
    /** Creates a new instance of HarvestableRefConverter */
    public HarvestableRefConverter() {
    }

    /**
     * Creates a new instance of HarvestableRefConverter.
     *
     * @param entity associated entity
     * @param uri associated uri
     * @param isUriExtendable indicates whether the uri can be extended
     */
    public HarvestableRefConverter(Harvestable entity, URI uri, boolean isUriExtendable) {
        this.entity = entity;
        this.uri = uri;
        this.isUriExtendable = isUriExtendable;
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
     * Returns the URI associated with this reference converter.
     *
     * @return the converted uri
     */
    @XmlAttribute(name = "uri")
    public URI getResourceUri() {
        if (isUriExtendable) {
            return UriBuilder.fromUri(uri).path(entity.getId() + "/").build();
        }
        return uri;
    }

    /**
     * Sets the URI for this reference converter.
     *
     */
    public void setResourceUri(URI uri) {
        this.uri = uri;
    }

    /**
     * Returns the Harvestable entity.
     *
     * @return Harvestable entity
     */
    @XmlTransient
    public Harvestable getEntity() {
        HarvestableConverter result = UriResolver.getInstance().resolve(HarvestableConverter.class, uri);
        if (result != null) {
            return result.getEntity();
        }
        return null;
    }
}
