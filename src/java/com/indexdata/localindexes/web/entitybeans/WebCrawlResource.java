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
@XmlRootElement(name="webCrawl")
public class WebCrawlResource extends Harvestable implements Serializable {
    private String harvestedUrls;
    private String filetypeMask;
    private String uriMask;

    public String getHarvestedUrls() {
        return harvestedUrls;
    }

    public void setHarvestedUrls(String harvestedUrls) {
        this.harvestedUrls = harvestedUrls;
    }

    public String getFiletypeMask() {
        return filetypeMask;
    }

    public void setFiletypeMask(String filetypeMask) {
        this.filetypeMask = filetypeMask;
    }

    public String getUriMask() {
        return uriMask;
    }

    public void setUriMask(String uriMask) {
        this.uriMask = uriMask;
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
        if (!(object instanceof WebCrawlResource)) {
            return false;
        }
        WebCrawlResource other = (WebCrawlResource) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.indexdata.localindexes.web.entitybeans.WebCrawlResource[id=" + id + "]";
    }

}
