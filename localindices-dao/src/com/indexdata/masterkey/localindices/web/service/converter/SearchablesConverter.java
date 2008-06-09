/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service.converter;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import java.net.URI;
import java.util.Collection;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlAttribute;
import java.util.ArrayList;


/**
 *
 * @author jakub
 */

@XmlRootElement(name = "searchables")
public class SearchablesConverter {
    private Collection<String> zurls;
    private URI uri;
    
    /** Creates a new instance of HarvestablesConverter */
    public SearchablesConverter() {
    }

    /**
     * Creates a new instance of HarvestablesConverter.
     *
     * @param entities associated entities
     * @param uri associated uri
     */
    public SearchablesConverter(Collection<Harvestable> entities, URI uri) {
        this.zurls = new ArrayList<String>();
        for (Harvestable entity : entities) {
            zurls.add("localhost:9999/job" + entity.getId());
        }
        this.uri = uri;
    }

    /**
     * Returns a collection of HarvestableRefConverter.
     *
     * @return a collection of HarvestableRefConverter
     */
    @XmlElement(name = "zurl")
    public Collection<String> getZurls() {
        return zurls;
    }

    /**
     * Sets a collection of HarvestableRefConverter.
     *
     * @param references a collection of HarvestableRefConverter to set
     */
    public void setZurls(Collection<String> zurls) {
        this.zurls = zurls;
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
    
}
