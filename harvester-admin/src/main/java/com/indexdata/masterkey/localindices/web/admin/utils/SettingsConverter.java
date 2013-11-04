/*
 * Copyright (c) 1995-2013, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.web.admin.utils;

import com.indexdata.masterkey.localindices.entity.Setting;
import com.indexdata.masterkey.localindices.web.admin.utils.select.SelectItemsBaseConverter;
import javax.faces.bean.ManagedBean;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author jakub
 */
@FacesConverter("settingsConverter")
public class SettingsConverter extends SelectItemsBaseConverter {
    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        return ((Setting) value).getId().toString();
    }
}
