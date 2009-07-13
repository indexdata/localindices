/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service.converter;

import com.indexdata.torus.Layer;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A "definition" of the searchable type.
 * @author jakub
 */
@XmlRootElement(name="layer")
public class SearchableTypeLayer extends Layer {
    private String name;
    private String zurl;
    private String transform;
    private String elementSet;
    private String requestSyntax;
    private String queryEncoding;
    private String recordEncoding;
    private String cclMapAu;
    private String cclMapTi;
    private String cclMapSu;
    private String cclMapDate;
    private String cclMapTerm;
    private String authentication;
    private String urlRecipe;
    private String serviceProvider;

    public String getRecordEncoding() {
        return recordEncoding;
    }

    public void setRecordEncoding(String recordEncoding) {
        this.recordEncoding = recordEncoding;
    }

    public String getRequestSyntax() {
        return requestSyntax;
    }

    public void setRequestSyntax(String requestSyntax) {
        this.requestSyntax = requestSyntax;
    }

    public String getTransform() {
        return transform;
    }    
    
    public void setTransform(String transform) {
        this.transform = transform;
    }
                        
    public void setElementSet(String elementSet) {
        this.elementSet = elementSet;
    }
    
    public String getElementSet() {
        return elementSet;
    }
        
    @XmlElement(name="displayName")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getZurl() {
        return zurl;
    }

    public void setZurl(String zurl) {
        this.zurl = zurl;
    }
    @XmlElement(name="cclmap_au")
    public String getCclMapAu() {
        return cclMapAu;
    }

    public void setCclMapAu(String cclMapAu) {
        this.cclMapAu = cclMapAu;
    }
    @XmlElement(name="cclmap_date")
    public String getCclMapDate() {
        return cclMapDate;
    }

    public void setCclMapDate(String cclMapDate) {
        this.cclMapDate = cclMapDate;
    }
    @XmlElement(name="cclmap_su")
    public String getCclMapSu() {
        return cclMapSu;
    }

    public void setCclMapSu(String cclMapSu) {
        this.cclMapSu = cclMapSu;
    }
    @XmlElement(name="cclmap_term")
    public String getCclMapTerm() {
        return cclMapTerm;
    }

    public void setCclMapTerm(String cclMapTerm) {
        this.cclMapTerm = cclMapTerm;
    }
    @XmlElement(name="cclmap_ti")
    public String getCclMapTi() {
        return cclMapTi;
    }

    public void setCclMapTi(String cclMapTi) {
        this.cclMapTi = cclMapTi;
    }

    public String getQueryEncoding() {
        return queryEncoding;
    }

    public void setQueryEncoding(String queryEncoding) {
        this.queryEncoding = queryEncoding;
    }

    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    public String getUrlRecipe() {
        return urlRecipe;
    }

    public void setUrlRecipe(String urlRecipe) {
        this.urlRecipe = urlRecipe;
    }

    public String getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(String serviceProvider) {
        this.serviceProvider = serviceProvider;
    }    
    
}
