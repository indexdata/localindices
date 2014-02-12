/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.dao.bean.HarvestablesDAOJPA;
import com.indexdata.masterkey.localindices.web.service.converter.SearchableConverter;

/**
 * REST Web service (resource) that maps to a Harvestable entity.
 * 
 * @author jakub
 * @author Dennis
 */
public class SearchableResource {
  Logger logger = Logger.getLogger(this.getClass());    
  private HarvestableDAO dao = new HarvestablesDAOJPA();
  private Long id;
  private UriInfo context;

  /** Creates a new instance of HarvestableResource */
  public SearchableResource() {
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
  public SearchableResource(Long id, UriInfo context) {
    this.id = id;
    this.context = context;
  }

  /**
   * Get method for retrieving an instance of referenced Harvestable in XML
   * format.
   * 
   * @return an instance of HarvestableConverter
   */
  @GET
  @Produces("application/xml")
  public SearchableConverter get() {
    return new SearchableConverter(dao.retrieveById(id), context.getAbsolutePath());
  }

}
