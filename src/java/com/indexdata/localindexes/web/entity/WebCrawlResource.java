/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.indexdata.localindexes.web.entity;

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
    // spce-delimited list
    private String startUrls;
    private String filetypeMasks;
    // space-delimited list
    private String uriMasks;
    private Integer recursionDepth;

    public String getFiletypeMasks() {
        return filetypeMasks;
    }

    public void setFiletypeMasks(String filetypeMasks) {
        this.filetypeMasks = filetypeMasks;
    }

    public Integer getRecursionDepth() {
        return recursionDepth;
    }

    public void setRecursionDepth(Integer recursionDepth) {
        this.recursionDepth = recursionDepth;
    }

    public String getStartUrls() {
        return startUrls;
    }

    public void setStartUrls(String startUrls) {
        this.startUrls = startUrls;
    }

    public String getUriMasks() {
        return uriMasks;
    }

    public void setUriMasks(String uriMasks) {
        this.uriMasks = uriMasks;
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
        return "com.indexdata.localindexes.web.entity.WebCrawlResource[id=" + id + "]";
    }

}
