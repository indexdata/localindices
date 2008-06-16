/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.torus;

import java.net.URI;
import java.util.Collection;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a single TORUS record.
 * @author jakub
 */
@XmlRootElement(name="record")
public class Record {
    private String type;
    private URI uri;
    private Collection<Layer> layers;

    public Record() {
    }

    public Record(String type) {
        this.type = type;
    }
    
    @XmlAttribute
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    @XmlAttribute
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
    @XmlElement(name="layer")
    public Collection<Layer> getLayers() {
        return layers;
    }

    public void setLayers(Collection<Layer> layers) {
        this.layers = layers;
    }
}
