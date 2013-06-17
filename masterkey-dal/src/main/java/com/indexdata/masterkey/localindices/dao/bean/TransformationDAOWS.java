/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.TransformationDAO;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationBrief;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationConverter;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationsConverter;
import com.indexdata.rest.client.ResourceConnector;

/**
 *
 * @author Dennis
 */
public class TransformationDAOWS extends CommonDAOWS implements TransformationDAO {

    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");

    public TransformationDAOWS(String serviceBaseURL) {
        super(serviceBaseURL);
    }

    /**
     * create (POST) entity to the Web Service
	 * @param Transformation
     * @return
     */
    @Override
    public void create(Transformation transformation) {
        try {
            ResourceConnector<TransformationsConverter> connector =
                    new ResourceConnector<TransformationsConverter>(
                    new URL(serviceBaseURL),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
        TransformationConverter converter = new TransformationConverter();
        converter.setEntity(transformation);
        URL url = connector.postAny(converter);
        transformation.setId(extractId(url));
        } catch (Exception male) {
            logger.error("Error creating transformation", male);
        }
    }

    /**
     * Retrieve (GET) entity from the Web Service
	 * @param id of the entity
     * @return Transformation
     */
    @Override
    public Transformation retrieveById(Long id) {
    	Transformation entity = null;
        try {
            ResourceConnector<TransformationConverter> storageConnector =
                new ResourceConnector<TransformationConverter>(
                    new URL(serviceBaseURL + id + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            entity = storageConnector.get().getEntity();
        } catch (Exception male) {
            logger.error("Error retrieving transformation", male);
        }
        return entity;    
    }

    /**
     * Retrieve list of all storages from the Web Service
     * @return
     */
    @Override
    public List<TransformationBrief> retrieveBriefs(int start, int max) {
        String url = serviceBaseURL + "?start=" + start + "&max=" + max;
        try {
            ResourceConnector<TransformationsConverter> storagesConnector =
                    new ResourceConnector<TransformationsConverter>(
                    new URL(url),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            TransformationsConverter hc = storagesConnector.get();
            return hc.getReferences();
        } catch (Exception male) {
            logger.error("Error listing transformations", male);
        }
        return null;
    }


    /**
     * Retrieve Transformation from the Web Service using it's reference (URL)
     * @param href storageRef entity
     * @return Transformation entity
     */
    @Override
    public Transformation retrieveFromBrief(TransformationBrief href) {
        try {
            ResourceConnector<TransformationConverter> connector =
                    new ResourceConnector<TransformationConverter>(
                    href.getResourceUri().toURL(),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            return connector.get().getEntity();
        } catch (Exception male) {
            logger.error("Error retrieving transformation", male);
        }
        return null;
    } // retrieveFromBrief

    /**
     * PUT Transformation to the Web Service
     * @param Transformation entity to be put
     */
    @Override
    public Transformation update(Transformation transformation) {
        try {
            ResourceConnector<TransformationConverter> connector =
                    new ResourceConnector<TransformationConverter>(
                    new URL(serviceBaseURL + transformation.getId() + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            TransformationConverter converter = new TransformationConverter();
            converter.setEntity(transformation);
            connector.put(converter);
        } catch (Exception male) {
            logger.error("Error updating transformation", male);
        }
        return transformation	;
    } // updateJob


    @Override
    public void delete(Transformation transformation) {
        try {
            ResourceConnector<TransformationConverter> connector =
                new ResourceConnector<TransformationConverter>(
                    new URL(serviceBaseURL + transformation.getId() + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            connector.delete();
        } catch (Exception male) {
            logger.error("Error deleting transformation", male);
        }
    }

    @Override
    public List<Transformation> retrieve(int start, int max) {
       //TODO this cannot be more stupid
       logger.log(Level.WARN, "This method id deprecetated and should not be used, use retrieveStorageBrief instead.");
       List<Transformation> hables = new ArrayList<Transformation>();
       List<TransformationBrief> hrefs = retrieveBriefs(start, max);
       if (hrefs != null) {
            for (TransformationBrief href : hrefs) {
            	Transformation hable = retrieveFromBrief(href);
                hables.add(hable);
            }
       }
       return hables;    
    }
    
    @Override
    public int getCount() {
        String url = serviceBaseURL + "?start=0&max=0";
        try {
            ResourceConnector<TransformationsConverter> connector =
                    new ResourceConnector<TransformationsConverter>(
                    new URL(url),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            TransformationsConverter converter = connector.get();
            return converter.getCount();
        } catch (Exception male) {
            logger.error("Error retrieving transformation count", male);
            return 0;
        }
        
    }
}
