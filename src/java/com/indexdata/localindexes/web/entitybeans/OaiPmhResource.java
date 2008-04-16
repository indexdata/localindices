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
@XmlRootElement(name="oaiPmh")
public class OaiPmhResource extends Harvestable implements Serializable {
    //url
    private String oaiSetName;
    private String metadataPrefix;
    private String schemaURI;
    private String normalizationFilter;

    public String getMetadataPrefix() {
        return metadataPrefix;
    }

    public void setMetadataPrefix(String metadataPrefix) {
        this.metadataPrefix = metadataPrefix;
    }

    public String getNormalizationFilter() {
        return normalizationFilter;
    }

    public void setNormalizationFilter(String normalizationFilter) {
        this.normalizationFilter = normalizationFilter;
    }

    public String getOaiSetName() {
        return oaiSetName;
    }

    public void setOaiSetName(String oaiSetName) {
        this.oaiSetName = oaiSetName;
    }

    public String getSchemaURI() {
        return schemaURI;
    }

    public void setSchemaURI(String schemaURI) {
        this.schemaURI = schemaURI;
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
        if (!(object instanceof OaiPmhResource)) {
            return false;
        }
        OaiPmhResource other = (OaiPmhResource) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.indexdata.localindexes.web.entitybeans.OaiPmhResource[id=" + id + "]";
    }

}
