/*
 * Copyright (c) 1995-2008, Index Data
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
import com.indexdata.masterkey.localindices.dao.StorageDAO;
import com.indexdata.masterkey.localindices.dao.bean.StoragesDAOJPA;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.web.service.converter.StorageConverter;
import com.indexdata.masterkey.localindices.web.service.converter.StoragesConverter;

/**
 * RESTful WS (resource) that maps to the Storage collection.
 * 
 * @author Dennis
 */
@Path("/storages/")
public class StoragesResource {
  private StorageDAO dao = new StoragesDAOJPA();
  @Context
  private UriInfo context;

  /** Creates a new instance of StoragesResource */
  public StoragesResource() {
  }

  /**
   * Constructor used for instantiating an instance of the Storages resource.
   * 
   * @param context
   *          HttpContext inherited from the parent resource
   */
  public StoragesResource(UriInfo context) {
    this.context = context;
  }

  /**
   * Get method for retrieving a collection of Storage instance in XML format.
   * 
   * @param start
   *          optional start item argument
   * @param max
   *          optional max results argument
   * @param acl
   * @return an instance of StorageConverter
   */
  @GET
  @Produces("application/xml")
  public StoragesConverter get(

  @QueryParam("start") @DefaultValue("0") int start,
  @QueryParam("max") @DefaultValue("100") int max,
  @QueryParam("acl") @DefaultValue("") String acl,
  @QueryParam("query") @DefaultValue("") String query) {
    EntityQuery entityQuery = new EntityQuery().withAcl(acl).withQuery(query);
    List<Storage> entities;
    if (max <= 0)
      entities = new ArrayList<Storage>();
    else
      entities = dao.retrieve(start, max, entityQuery);
    return new StoragesConverter(entities, context.getAbsolutePath(), start, max, dao.getCount(entityQuery));
  }

  /**
   * Post method for creating an instance of Storage using XML as the input
   * format.
   * 
   * @param data
   *          an StorageConverter entity that is deserialized from an XML stream
   * @return Http 201 response code.
   */
  @POST
  @Consumes("application/xml")
  public Response post(StorageConverter data) {
    Storage entity = data.getEntity();
    entity.setCurrentStatus("NEW");
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
  public StorageResource getStorageResource(@PathParam("id") Long id) {
    return new StorageResource(id, context);
  }
}
