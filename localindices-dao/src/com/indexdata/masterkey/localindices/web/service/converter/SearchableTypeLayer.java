/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service.converter;

import com.indexdata.torus.Layer;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A "definition" of the searchable type.
 * @author jakub
 */
@XmlRootElement(name="layer")
public class SearchableTypeLayer extends Layer {
    private String name;
    private String zurl;
    
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
}
