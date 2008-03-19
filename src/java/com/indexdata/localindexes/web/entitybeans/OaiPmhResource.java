/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.indexdata.localindexes.web.entitybeans;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author jakub
 */
@Entity
@Table(name = "OaiPmhResource")
@NamedQueries({@NamedQuery(name = "OaiPmhResource.findById", query = "SELECT o FROM OaiPmhResource o WHERE o.id = :id"), @NamedQuery(name = "OaiPmhResource.findByName", query = "SELECT o FROM OaiPmhResource o WHERE o.name = :name"), @NamedQuery(name = "OaiPmhResource.findByLastUpdated", query = "SELECT o FROM OaiPmhResource o WHERE o.lastUpdated = :lastUpdated")})
public class OaiPmhResource implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "lastUpdated", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdated;

    public OaiPmhResource() {
    }

    public OaiPmhResource(Integer id) {
        this.id = id;
    }

    public OaiPmhResource(Integer id, String name, Date lastUpdated) {
        this.id = id;
        this.name = name;
        this.lastUpdated = lastUpdated;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
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
