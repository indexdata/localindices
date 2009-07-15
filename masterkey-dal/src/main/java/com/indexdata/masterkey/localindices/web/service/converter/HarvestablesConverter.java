/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service.converter;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import java.net.URI;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAttribute;
import java.util.ArrayList;


/**
 *
 * @author jakub
 */

@XmlRootElement(name = "harvestables")
public class HarvestablesConverter {
    private List<HarvestableBrief> references;
    private URI uri;
    private int start;
    private int max;
    private int count;
    
    /** Creates a new instance of HarvestablesConverter */
    public HarvestablesConverter() {
    }

    /**
     * Creates a new instance of HarvestablesConverter.
     *
     * @param entities associated entities
     * @param uri associated uri
     */
    public HarvestablesConverter(List<Harvestable> entities, URI uri, int start, int max, int count) {
        this.references = new ArrayList<HarvestableBrief>();
        for (Harvestable entity : entities) {
            references.add(new HarvestableBrief(entity, uri, true));
        }
        this.uri = uri;
        this.start = start;
        this.max = max;
        this.count = count;
    }

    /**
     * Returns a collection of HarvestableBrief.
     *
     * @return a collection of HarvestableBrief
     */
    @XmlElement(name = "harvestableBrief")
    public List<HarvestableBrief> getReferences() {
        return references;
    }

    /**
     * Sets a collection of HarvestableBrief.
     *
     * @param references a collection of HarvestableBrief to set
     */
    public void setReferences(List<HarvestableBrief> references) {
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

    @XmlAttribute(name = "max")
    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }
    
    @XmlAttribute(name = "start")
    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }
    
    @XmlAttribute(name = "count")
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }    
}
