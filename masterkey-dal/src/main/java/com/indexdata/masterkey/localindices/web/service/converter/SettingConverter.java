/*
 * Copyright (c) 1995-2013, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.web.service.converter;

import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import com.indexdata.masterkey.localindices.entity.Setting;

/**
 *
 * @author jakub
 */
@XmlRootElement(name = "settings-item")
public class SettingConverter {
  private Setting entity;
  private URI uri;

  public SettingConverter() {
  }

  public SettingConverter(Setting entity) {
    this.entity = entity;
  }

  public SettingConverter(Setting entity, URI uri) {
    this.entity = entity;
    this.uri = uri;
  }

  @XmlAttribute(name = "uri")
  public URI getResourceUri() {
    return uri;
  }

  public void setResourceUri(URI uri) {
    this.uri = uri;
  }

  @XmlElementRef
  public Setting getEntity() {
    return entity;
  }

  public void setEntity(Setting entity) {
    this.entity = entity;
  }
}
