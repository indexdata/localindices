/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service.converter;

import com.indexdata.masterkey.localindices.entity.Transformation;
import java.net.URI;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAttribute;
import java.util.ArrayList;


/**
 *
 * @author Dennis
 */

@XmlRootElement(name = "tranformations")
public class TransformationsConverter {
    private List<TransformationBrief> references;
    private URI uri;
    private int start;
    private int max;
    private int count;
    
    /** Creates a new instance of StoragesConverter */
    public TransformationsConverter() {
    }

    /**
     * Creates a new instance of StoragesConverter.
     *
     * @param entities associated entities
     * @param uri associated uri
     */
    public TransformationsConverter(List<Transformation> entities, URI uri, int start, int max, int count) {
        this.references = new ArrayList<TransformationBrief>();
        for (Transformation entity : entities) {
            references.add(new TransformationBrief(entity /* TODO fix, uri, true */));
        }
        this.uri = uri;
        this.start = start;
        this.max = max;
        this.count = count;
    }

    /**
     * Returns a collection of StorageBrief.
     *
     * @return a collection of StorageBrief
     */
    @XmlElement(name = "harvestableBrief")
    public List<TransformationBrief> getReferences() {
        return references;
    }

    /**
     * Sets a collection of StorageBrief.
     *
     * @param references a collection of StorageBrief to set
     */
    public void setReferences(List<TransformationBrief> references) {
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
