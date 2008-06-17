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
 * A collection of TORUS records for listing purposes.
 * @author jakub
 */
@XmlRootElement(name="records")
public class Records {
    private Collection<Record> records;
    private URI uri;
    
    @XmlElement(name="record")
    final public Collection<Record> getRecords() {
        return records;
    }

    final public void setRecords(Collection<Record> records) {
        this.records = records;
    }
    
    @XmlAttribute
    final public URI getUri() {
        return uri;
    }

    final public void setUri(URI uri) {
        this.uri = uri;
    }
    
}
