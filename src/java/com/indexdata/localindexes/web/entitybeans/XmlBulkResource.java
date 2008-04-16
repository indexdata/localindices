/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.indexdata.localindexes.web.entitybeans;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author jakub
 */
@Entity
@XmlRootElement(name="xmlBulk")
public class XmlBulkResource extends Harvestable implements Serializable {
    private String url;
    private String expectedSchema;
    private String normalizationFilter;

    public String getExpectedSchema() {
        return expectedSchema;
    }

    public void setExpectedSchema(String expectedSchema) {
        this.expectedSchema = expectedSchema;
    }

    public String getNormalizationFilter() {
        return normalizationFilter;
    }

    public void setNormalizationFilter(String normalizationFilter) {
        this.normalizationFilter = normalizationFilter;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
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

    @Override
    public String toString() {
        return "com.indexdata.localindexes.web.entitybeans.XmlBulkResource[id=" + id + "]";
    }

}
