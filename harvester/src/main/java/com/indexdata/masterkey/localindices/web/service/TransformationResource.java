/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.UriInfo;

import com.indexdata.masterkey.localindices.dao.TransformationDAO;
import com.indexdata.masterkey.localindices.dao.bean.TransformationsDAOJPA;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationConverter;

/**
 * REST Web service (resource) that maps to a Transformation entity.
 * 
 * @author Dennis
 */
public class TransformationResource {
  private TransformationDAO dao = new TransformationsDAOJPA();
  private Long id;
  private UriInfo context;

  /** Creates a new instance of HarvestableResource */
  public TransformationResource() {
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
  public TransformationResource(Long id, UriInfo context) {
    this.id = id;
    this.context = context;
  }

  /**
   * Get method for retrieving an instance of referenced Storage in XML format.
   * 
   * @return an instance of StorageConverter
   */
  @GET
  @Produces("application/xml")
  public TransformationConverter get() {
    return new TransformationConverter(dao.retrieveById(id), context.getAbsolutePath());
  }

  /**
   * Put method for updating an instance of referenced Transformation, using XML
   * as the input format.
   * 
   * @param data
   *          an HarvestableConverter entity that is de-serialized from an XML
   *          stream
   */
  @PUT
  @Consumes("application/xml")
  public void put(TransformationConverter data) {
    Transformation entity = data.getEntity();
    dao.update(entity);
  }

  /**
   * Delete method for deleting an instance of referenced Harvestable.
   * 
   */
  @DELETE
  public void delete() {
    dao.delete(dao.retrieveById(id));
  }

}
