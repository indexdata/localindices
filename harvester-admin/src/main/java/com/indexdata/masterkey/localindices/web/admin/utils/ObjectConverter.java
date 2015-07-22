/*
 * Copyright (c) 1995-2013, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.web.admin.utils;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

import com.indexdata.masterkey.localindices.web.admin.utils.select.SelectItemsBaseConverter;

/**
 * Simple converter that uses overlaoded toString to serialize and compare instances
 * @author jakub
 */
@FacesConverter("objectConverter")
public class ObjectConverter extends SelectItemsBaseConverter {
  
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        return value.toString();
    }
    
}
