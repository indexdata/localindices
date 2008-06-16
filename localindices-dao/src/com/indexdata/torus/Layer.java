/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.torus;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a single layer within a record.
 * @author jakub
 */
@XmlRootElement(name="layer")
public abstract class Layer {
    private String layerName;
        
    public Layer() {
    }

    public Layer(String name) {
        layerName = name;
    }
    @XmlAttribute(name="name")
    public String getLayerName() {
        return layerName;
    }

    public void seLayertName(String name) {
        layerName = name;
    }
}
