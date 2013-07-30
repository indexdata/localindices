/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.dao.bean.HarvestablesDAOJPA;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableConverter;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestablesConverter;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

/**
 * RESTful WS (resource) that maps to the Harvestables collection.
 * 
 * @author jakub
 */
@Path("/harvestables/")
public class HarvestablesResource {
  private HarvestableDAO dao = new HarvestablesDAOJPA();
  @Context
  private UriInfo context;

  /** Creates a new instance of HarvestablesResource */
  public HarvestablesResource() {
  }

  /**
   * Constructor used for instantiating an instance of the Harvestables
   * resource.
   * 
   * @param context
   *          HttpContext inherited from the parent resource
   */
  public HarvestablesResource(UriInfo context) {
    this.context = context;
  }

  /**
   * Get method for retrieving a collection of Harvestable instance in XML
   * format.
   * 
   * @param start
   *          optional start item argument
   * @param max
   *          optional max results argument
   * @return an instance of HarvestablesConverter
   */
  @GET
  @Produces("application/xml")
  public HarvestablesConverter get(
  @QueryParam("start") @DefaultValue("0") int start,
  @QueryParam("max") @DefaultValue("100") int max,
  @QueryParam("sort") @DefaultValue("") String sortSpec) {
    String sortKey = null;
    boolean isAsc = true;
    if (!sortSpec.isEmpty()) {
      isAsc = !sortSpec.startsWith("~");
      sortKey = isAsc ? sortSpec : sortSpec.substring(1);
    }
    List<Harvestable> entities;
    if (max <= 0)
      entities = new ArrayList<Harvestable>();
    else
      entities = dao.retrieve(start, max, sortKey, isAsc);
    return new HarvestablesConverter(entities, context.getAbsolutePath(), start, max,
	dao.getCount());
  }

  /**
   * Post method for creating an instance of Harvestable using XML as the input
   * format.
   * 
   * @param data
   *          an HarvestableConverter entity that is deserialized from an XML
   *          stream
   * @return Http 201 response code.
   */
  @POST
  @Consumes("application/xml")
  public Response post(HarvestableConverter data) {
    Harvestable entity = data.getEntity();
    entity.setCurrentStatus("NEW");
    dao.create(entity);
    return Response.created(context.getAbsolutePath().resolve(entity.getId() + "/")).build();
  }

  /**
   * Entry point to the Harvestable WS.
   * 
   * @param id
   *          resource id
   * @return an instance of HarvestableResource (WS)
   */
  @Path("{id}/")
  public HarvestableResource getHarvestableResource(@PathParam("id") Long id) {
    return new HarvestableResource(id, context);
  }
}
