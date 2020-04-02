/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.entity;

import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author kurt
 */
@Entity
@XmlRootElement(name="inventoryStorage")
public class InventoryStorageEntity extends Storage {

  private static final long serialVersionUID = -7498460780910228665L;

  @Override
  public String getSearchUrl(Harvestable resource) {
    if (resource == null)
      	return super.getSearchUrl();
    StringBuffer clientUrl = new StringBuffer(super.getSearchUrl());
    if (clientUrl.lastIndexOf("/") + 1 != clientUrl.length())
      clientUrl.append('/');
    clientUrl.append(resource.getId());
    return clientUrl.toString();
  }

}
