/*
 * Copyright (c) 1995-2013, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.web.service;

import com.indexdata.masterkey.localindices.dao.EntityInUse;
import com.indexdata.masterkey.localindices.dao.SettingDAO;
import com.indexdata.masterkey.localindices.dao.bean.SettingDAOJPA;
import com.indexdata.masterkey.localindices.entity.Setting;
import com.indexdata.masterkey.localindices.web.service.converter.SettingConverter;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriInfo;

/**
 *
 * @author jakub
 */
public class SettingResource {
  private SettingDAO dao = new SettingDAOJPA();
  private Long id;
  private UriInfo context;

  public SettingResource() {
  }
  
  public SettingResource(Long id, UriInfo context) {
    this.id = id;
    this.context = context;
  }

  @GET
  @Produces("application/xml")
  public SettingConverter get() {
    return new SettingConverter(dao.retrieveById(id), context.getAbsolutePath());
  }

  /**
   * Put method for updating an instance of referenced entity, using XML as the
   * input format.
   * 
   * @param data
   *          an HarvestableConverter entity that is deserialized from an XML
   *          stream
   */
  @PUT
  @Consumes("application/xml")
  public void put(SettingConverter data) {
    Setting entity = data.getEntity();
    dao.update(entity);
  }

  /**
   * Delete method for deleting an instance of referenced entity.
   * 
   */
  @DELETE
  public void delete() {
    try {
      dao.delete(dao.retrieveById(id));
    } catch (EntityInUse eiu) {
      throw new WebApplicationException(WSUtils.buildError(400, EntityInUse.ERROR_MESSAGE));
    }
  }

}
