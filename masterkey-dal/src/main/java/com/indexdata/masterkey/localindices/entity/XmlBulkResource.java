/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author jakub
 */
@Entity
@XmlRootElement(name="xmlBulk")
public class XmlBulkResource extends Harvestable implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = -6751028242629873367L;
	@Column(length=16384)
    private String url;
    private String expectedSchema;

	public XmlBulkResource() {
	}

	public XmlBulkResource(String url) {
		this.url = url;
	}
    public String getExpectedSchema() {
        return expectedSchema;
    }

    public void setExpectedSchema(String expectedSchema) {
        this.expectedSchema = expectedSchema;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof XmlBulkResource)) {
            return false;
        }
        XmlBulkResource other = (XmlBulkResource) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

}
