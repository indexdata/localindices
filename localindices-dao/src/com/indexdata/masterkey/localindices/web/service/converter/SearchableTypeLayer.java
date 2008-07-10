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
<<<<<<< HEAD:localindices-dao/src/com/indexdata/masterkey/localindices/web/service/converter/SearchableTypeLayer.java
    private String transform;
=======
    private String transformStlylesheet;
    private String elementSet;
>>>>>>> a141494abc90b929463f70f89fb690a868ee75ce:localindices-dao/src/com/indexdata/masterkey/localindices/web/service/converter/SearchableTypeLayer.java

<<<<<<< HEAD:localindices-dao/src/com/indexdata/masterkey/localindices/web/service/converter/SearchableTypeLayer.java
    public String getTransform() {
        return transform;
=======
    public void setElementSet(String elementSet) {
        this.elementSet = elementSet;
>>>>>>> a141494abc90b929463f70f89fb690a868ee75ce:localindices-dao/src/com/indexdata/masterkey/localindices/web/service/converter/SearchableTypeLayer.java
    }

<<<<<<< HEAD:localindices-dao/src/com/indexdata/masterkey/localindices/web/service/converter/SearchableTypeLayer.java
    public void setTransform(String transform) {
        this.transform = transform;
=======
    public String getElementSet() {
        return elementSet;
>>>>>>> a141494abc90b929463f70f89fb690a868ee75ce:localindices-dao/src/com/indexdata/masterkey/localindices/web/service/converter/SearchableTypeLayer.java
    }
<<<<<<< HEAD:localindices-dao/src/com/indexdata/masterkey/localindices/web/service/converter/SearchableTypeLayer.java
    
    @XmlElement(name="displayName")
=======
   
>>>>>>> a141494abc90b929463f70f89fb690a868ee75ce:localindices-dao/src/com/indexdata/masterkey/localindices/web/service/converter/SearchableTypeLayer.java
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
