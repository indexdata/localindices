/*
 * Copyright (c) 1995-2013, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.web.service;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.indexdata.masterkey.localindices.dao.EntityQuery;
import com.indexdata.masterkey.localindices.dao.SettingDAO;
import com.indexdata.masterkey.localindices.dao.bean.SettingDAOJPA;
import com.indexdata.masterkey.localindices.entity.Setting;
import com.indexdata.masterkey.localindices.web.service.converter.SettingConverter;
import com.indexdata.masterkey.localindices.web.service.converter.SettingsConverter;

/**
 * Harvester settings web-service endpoint
 * @author jakub
 */
@Path("/settings/")
public class SettingsResource {
  private SettingDAO dao = new SettingDAOJPA();
  @Context
  private UriInfo context;

  public SettingsResource() {
  }

  public SettingsResource(UriInfo context) {
    this.context = context;
  }

  /**
   * Get method for retrieving a collection of Settings.
   *
   * @param start
   *          optional start item argument
   * @param max
   *          optional max results argument
   * @return an instance of SettingsConverter
   */
  @GET
  @Produces("application/xml")
  public SettingsConverter get(
  @QueryParam("start") @DefaultValue("0") int start,
  @QueryParam("max") @DefaultValue("100") int max,
  @QueryParam("prefix") String prefix,
  @QueryParam("acl") String acl) {
    EntityQuery query = new EntityQuery().withAcl(acl);
    List<Setting> entities;
    if (max <= 0) {
      entities = new ArrayList<Setting>();
    } else {
      entities = prefix == null
        ? dao.retrieve(start, max, query)
        : dao.retrieve(start, max, prefix, false, query);
    }
    int count = prefix == null ? dao.getCount(query) : dao.getCount(prefix, query);
    return new SettingsConverter(entities, context.getAbsolutePath(), start, max, count);
  }

  /**
   * Post method for creating an instance of Setting.
   *
   * @param data
   *          an SettingConverter entity that is deserialized from an XML stream
   * @return Http 201 response code.
   */
  @POST
  @Consumes("application/xml")
  public Response post(SettingConverter data) {
    Setting entity = data.getEntity();
    dao.create(entity);
    return Response.created(context.getAbsolutePath().resolve(entity.getId() + "/")).build();
  }

  /**
   * Entry point to the Storage WS.
   *
   * @param id
   *          resource id
   * @return an instance of StorageResource (WS)
   */
  @Path("{id}/")
  public SettingResource getSettingResource(@PathParam("id") Long id) {
    return new SettingResource(id, context);
  }

}
