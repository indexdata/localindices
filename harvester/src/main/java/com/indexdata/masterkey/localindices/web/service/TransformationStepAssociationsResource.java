/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.indexdata.masterkey.localindices.dao.TransformationStepAssociationDAO;
import com.indexdata.masterkey.localindices.dao.bean.TransformationStepAssociationsDAOJPA;
import com.indexdata.masterkey.localindices.entity.TransformationStepAssociation;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepAssociationConverter;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepAssociationsConverter;

/**
 * RESTful WS (resource) that maps to the Transformation collection.
 * @author Dennis
 */
@Path("/tsa/")
public class TransformationStepAssociationsResource {
    private TransformationStepAssociationDAO dao = new TransformationStepAssociationsDAOJPA();
    @Context
    private UriInfo context;

    /** Creates a new instance of TransformationsResource */
    public TransformationStepAssociationsResource() {
    }

    /**
     * Constructor used for instantiating an instance of the Transformations resource.
     *
     * @param context HttpContext inherited from the parent resource
     */
    public TransformationStepAssociationsResource(UriInfo context) {
        this.context = context;
    }

    /**
     * Get method for retrieving a collection of Transformation instance in XML format.
     *
     * @param start optional start item argument
     * @param max optional max results argument
     * @return an instance of TransformationConverter
     */
    @GET
    @Produces("application/xml")
    @Path("{transid}/")
    public TransformationStepAssociationsConverter get(@PathParam("transid") Long id)
	{
	    List<TransformationStepAssociation> entities;
	    entities = dao.retrieveByTransformationId(id);
	    return new TransformationStepAssociationsConverter(entities, context.getAbsolutePath(),
	            dao.getStepCountByTransformationId(id));
    }

    /**
     * Post method for creating an instance of Transformation using XML as the input format.
     *
     * @param data an TransformationConverter entity that is deserialized from an XML stream
     * @return Http 201 response code.
     */
    @POST
    @Consumes("application/xml")
    public Response post(TransformationStepAssociationConverter data) {
        TransformationStepAssociation entity = data.getEntity();
        dao.create(entity);
        return Response.created(context.getAbsolutePath().resolve(entity.getId() + "/")).build();
    }

    /**
     * Entry point to the Transformation WS.
     *
     * @param id resource id
     * @return an instance of TransformationResource (WS)
     */
    @Path("{id}/")
    public TransformationStepAssociationResource getTransformationStepResource(            
    @PathParam("id") Long id) {
        return new TransformationStepAssociationResource(id, context);
    }
}
