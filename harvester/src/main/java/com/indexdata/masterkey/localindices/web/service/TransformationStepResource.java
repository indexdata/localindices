/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service;

import com.indexdata.masterkey.localindices.dao.EntityInUse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.UriInfo;

import com.indexdata.masterkey.localindices.dao.TransformationStepDAO;
import com.indexdata.masterkey.localindices.dao.bean.TransformationStepsDAOJPA;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepConverter;
import javax.ws.rs.WebApplicationException;

/**
 * REST Web service (resource) that maps to a TranformationStep entity.
 * 
 * @author Dennis
 */
public class TransformationStepResource {
  private TransformationStepDAO dao = new TransformationStepsDAOJPA();
  private Long id;
  private UriInfo context;

  /** Creates a new instance of TransformationStepResource */
  public TransformationStepResource() {
  }

  /**
   * Constructor used for instantiating an instance of the entity referenced by
   * id.
   * 
   * @param id
   *          identifier for referenced the entity
   * @param context
   *          HttpContext inherited from the parent resource
   */
  public TransformationStepResource(Long id, UriInfo context) {
    this.id = id;
    this.context = context;
  }

  /**
   * Get method for retrieving an instance of referenced Storage in XML format.
   * 
   * @return an instance of ...Converter
   */
  @GET
  @Produces("application/xml")
  public TransformationStepConverter get() {
    return new TransformationStepConverter(dao.retrieveById(id), context.getAbsolutePath());
  }

  /**
   * Put method for updating an instance of referenced entity, using XML as the
   * input format.
   * 
   * @param data
   *          an ...Converter entity that is de-serialized from an XML stream
   */
  @PUT
  @Consumes("application/xml")
  public void put(TransformationStepConverter data) {
    TransformationStep entity = data.getEntity();
    // TODO Fix
    dao.update(entity);
  }

  /**
   * Delete method for deleting an instance of referenced Harvestable.
   * 
   */
  @DELETE
  public void delete() {
    try {
      dao.delete(dao.retrieveById(id));
    } catch (EntityInUse eiu) {
      throw new WebApplicationException(WSUtils.buildError(400, "storage is in use"));
    }
  }

}
