/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.entity;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author kurt
 */
@Entity
@XmlRootElement(name="inventoryStorage")
public class InventoryStorageEntity extends Storage implements Serializable {

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
