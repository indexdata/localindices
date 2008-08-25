/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.torus.layerbean;

import com.indexdata.torus.Layer;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author jakub
 */
@XmlRootElement(name="layer")
public class IdentityTypeLayer extends Layer {
    private String identityId;

    public String getIdentityId() {
        return identityId;
    }

    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }
}
