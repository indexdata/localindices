/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.dao.bean.HarvestablesDAOJPA;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.storage.HarvestStorageFactory;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.util.HarvestableLog;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableConverter;
import javax.ws.rs.core.Response;

/**
 * REST Web service (resource) that maps to a Harvestable entity.
 * 
 * @author jakub
 * @author Dennis
 */
public class HarvestableResource {
  Logger logger = Logger.getLogger(this.getClass());    
  private HarvestableDAO dao = new HarvestablesDAOJPA();
  private Long id;
  private UriInfo context;

  /** Creates a new instance of HarvestableResource */
  public HarvestableResource() {
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
  public HarvestableResource(Long id, UriInfo context) {
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
  public HarvestableConverter get() {
    return new HarvestableConverter(dao.retrieveById(id), context.getAbsolutePath());
  }

  /**
   * Put method for updating an instance of referenced Harvestable, using XML as
   * the input format.
   * 
   * @param data
   *          an HarvestableConverter entity that is deserialized from an XML
   *          stream
   */
  @PUT
  @Consumes("application/xml")
  public void put(HarvestableConverter data) {
    Harvestable entity = data.getEntity();
    entity.setCurrentStatus("NEW");
    entity.setMessage(null);
    dao.update(entity);
  }

  private void purgeStorage(Harvestable harvestable) throws IOException {
    if (harvestable.getStorage() != null) {
      RecordStorage storage = HarvestStorageFactory.getStorage(harvestable);
      if (storage != null)
	storage.purge(true);
      else 
	logger.warn("No storage client. Unable to purge harvestable: " + id);
    }
    else 
      logger.log(Level.WARN, "No storage configured on harvestable " + id);
  }

  /**
   * Delete method for deleting an instance of referenced Harvestable.
   * 
   */
  @DELETE
  public void delete() {
    Harvestable harvestable = dao.retrieveById(id);
    if (harvestable != null) {
      try { 
	purgeStorage(harvestable);
	dao.delete(harvestable);
        DiskCache dc = new DiskCache(id);
        dc.purge();
	return ;
      } catch (Exception e) {
	logger.log(Level.ERROR, "Failed to delete records in storage " 
	    + harvestable.getStorage().getId(), e);
      }
    }
    logger.log(Level.ERROR, "No harvestable with id " + id); 
  }

  @Path("reset/")
  @GET
  @Produces("text/plain")
  public String reset() {
    Harvestable harvestable = dao.retrieveById(id);
    if (harvestable != null) {
      try { 
	purgeStorage(harvestable);
	harvestable.reset();
	dao.update(harvestable);
	return "OK"; 
      } catch (Exception e) {
	String error = "Failed to reset harvestable " + harvestable.getStorage().getId() + ": " + e.getMessage();
	logger.log(Level.ERROR, error, e);
	return error;
      }
    }
    return "No harvestable to reset with id: " + id;
  }
  
  @Path("cache/")
  @DELETE
  @Produces("text/plain")
  public String resetCache() {
    Harvestable harvestable = dao.retrieveById(id);
    if (harvestable == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    try { 
      DiskCache dc = new DiskCache(id);
      dc.purge();
      return "OK"; 
    } catch (Exception e) {
      String error = "Failed to reset cache for job '"+id+"'";
      logger.error(error, e);
      return error;
    }
  }

  /**
   * Entry point to the Harvestable log file.
   * 
   */
  @Path("log/")
  @GET
  @Produces("text/plain")
  public String getHarvestableLog() {
    try {
      return HarvestableLog.getHarvestableLog(id);
    } catch (FileNotFoundException fnf) {
      throw new WebApplicationException(fnf);
    } catch (IOException io) {
      throw new WebApplicationException(io);
    }
  }
}
