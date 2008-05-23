/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.localindexes.web.service.converter;

import com.indexdata.localindexes.web.entity.Harvestable;
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

@XmlRootElement(name = "harvestables")
public class HarvestablesConverter {
    private Collection<HarvestableRefConverter> references;
    private URI uri;
    
    /** Creates a new instance of HarvestablesConverter */
    public HarvestablesConverter() {
    }

    /**
     * Returns a collection of HarvestableRefConverter.
     *
     * @return a collection of HarvestableRefConverter
     */
    @XmlElement(name = "harvestableRef")
    public Collection<HarvestableRefConverter> getReferences() {
        return references;
    }

    /**
     * Sets a collection of HarvestableRefConverter.
     *
     * @param references a collection of HarvestableRefConverter to set
     */
    public void setReferences(Collection<HarvestableRefConverter> references) {
        this.references = references;
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
