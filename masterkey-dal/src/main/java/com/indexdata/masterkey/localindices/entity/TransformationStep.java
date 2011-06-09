/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * @author Dennis
 */
@Entity
@NamedQueries({@NamedQuery(name = "TransformationStep.findById", query = "SELECT o FROM TransformationStep o WHERE o.id = :id")})
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public abstract class TransformationStep implements Serializable, Cloneable {

    protected static final long serialVersionUID = 1L;
    // user-set properties
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;
    protected String name;
    @Column(length=4096)
    protected String description;
    protected Boolean enabled;
    @Lob
    protected String script = 
		"<?xml version=\"1.0\"?>\n" + 
		"<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" \n" + 
		"                xmlns:pz=\"http://www.indexdata.com/pazpar2/1.0\" >\n" + 		"\n" + 
		"  <xsl:param name=\"medium\" />\n" + 
		"  <xsl:template  match=\"/\">\n" + 
		"      <xsl:apply-templates></xsl:apply-templates>\n" + 
		"  </xsl:template>\n" + 
		"  <xsl:template match=\"doc\">\n" + 
		"    <pz:record>\n" + 
		"      <xsl:apply-templates></xsl:apply-templates>\n" + 
		"    </pz:record>\n" + 
		"  </xsl:template>\n" + 
		"  <xsl:template match=\"str[@name]\">\n" + 
		"    <pz:metadata>\n" + 
		"        <xsl:attribute  name=\"type\">\n" + 
		"          <xsl:value-of select=\"@name\"/>\n" + 
		"        </xsl:attribute>\n" + 
		"        <xsl:value-of select=\".\"/>\n" + 
		"    </pz:metadata>\n" + 
		"  </xsl:template>\n" + 
		"  <xsl:template match=\"arr\">\n" + 
		"    <xsl:for-each select=\"str\">\n" + 
		"      <xsl:call-template name=\"string\"/>\n" + 
		"    </xsl:for-each>\n" + 
		"  </xsl:template>\n" + 
		"  <xsl:template name=\"string\">\n" + 
		"      <pz:metadata>\n" + 
		"        <xsl:attribute  name=\"type\">\n" + 
		"          <xsl:value-of select=\"../@name\"/>\n" + 
		"        </xsl:attribute>\n" + 
		"        <xsl:choose>\n" + 
		"          <xsl:when test=\"../@name = 'medium' and string-length($medium) > 0\">\n" + 
		"            <xsl:value-of select=\"$medium\"/>\n" + 
		"          </xsl:when>\n" + 
		"          <xsl:otherwise>\n" + 
		"            <xsl:value-of select=\".\"/>\n" + 
		"          </xsl:otherwise>\n" + 
		"        </xsl:choose>\n" + 
		"      </pz:metadata>\n" + 
		"  </xsl:template>\n" + 
		"</xsl:stylesheet>";
    	
    protected Integer position;
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // change that so that it check the real class
        if (!(object instanceof Transformation)) {
            return false;
        }
        Transformation other = (Transformation) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + "[id=" + id + "]";
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

	public Integer getPosition() {
		return position;
	}

	public void setPosition(Integer position) {
		this.position = position;
	}
}
