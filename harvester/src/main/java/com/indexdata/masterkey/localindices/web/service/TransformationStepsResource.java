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

import com.indexdata.masterkey.localindices.dao.TransformationStepDAO;
import com.indexdata.masterkey.localindices.dao.bean.TransformationStepsDAOJPA;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepConverter;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepsConverter;

/**
 * RESTful WS (resource) that maps to the Transformation collection.
 * @author Dennis
 */
@Path("/steps/")
public class TransformationStepsResource {
    private TransformationStepDAO dao = new TransformationStepsDAOJPA();
    @Context
    private UriInfo context;

    /** Creates a new instance of TransformationsResource */
    public TransformationStepsResource() {
    }

    /**
     * Constructor used for instantiating an instance of the Transformations resource.
     *
     * @param context HttpContext inherited from the parent resource
     */
    public TransformationStepsResource(UriInfo context) {
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
    public TransformationStepsConverter get(
            
            @QueryParam("start")
            @DefaultValue("0") int start,
            
            @QueryParam("max")
            @DefaultValue("100") int max) {
        List<TransformationStep> entities;
        if (max <= 0)
            entities = new ArrayList<TransformationStep>();
        else
            entities = dao.retrieve(start, max);
        return new TransformationStepsConverter(entities, context.getAbsolutePath(),
                start, max, dao.getCount());
    }

    /**
     * Post method for creating an instance of Transformation using XML as the input format.
     *
     * @param data an TransformationConverter entity that is deserialized from an XML stream
     * @return Http 201 response code.
     */
    @POST
    @Consumes("application/xml")
    public Response post(TransformationStepConverter data) {
        TransformationStep entity = data.getEntity();
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
    public TransformationStepResource getTransformationStepResource(            
    @PathParam("id") Long id) {
        return new TransformationStepResource(id, context);
    }
}
