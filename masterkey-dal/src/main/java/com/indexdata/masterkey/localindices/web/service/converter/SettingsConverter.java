/*
 * Copyright (c) 1995-2013, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.web.service.converter;

import com.indexdata.masterkey.localindices.entity.Setting;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import static com.indexdata.utils.TextUtils.joinPath;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jakub
 */
@XmlRootElement(name = "settings")
public class SettingsConverter {
    private List<SettingConverter> references;
    private URI uri;
    private int start;
    private int max;
    private int count;
    
    public SettingsConverter() {
    }

    public SettingsConverter(List<Setting> entities, URI uri, int start, int max, int count) {
        this.references = new ArrayList<SettingConverter>();
        for (Setting entity : entities) {
          try {
            references.add(new SettingConverter(entity, new URI(joinPath(uri.toString(), entity.getStringId()))));
          } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
          }
        }
        this.uri = uri;
        this.start = start;
        this.max = max;
        this.count = count;
    }

    @XmlElementRef
    public List<SettingConverter> getReferences() {
        return references;
    }


    public void setReferences(List<SettingConverter> references) {
        this.references = references;
    }


    @XmlAttribute(name = "uri")
    public URI getResourceUri() {
        return uri;
    }

    public void setResourceUri(URI uri) {
        this.uri = uri;
    }

    @XmlAttribute(name = "max")
    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }
    
    @XmlAttribute(name = "start")
    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }
    
    @XmlAttribute(name = "count")
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }    
}
