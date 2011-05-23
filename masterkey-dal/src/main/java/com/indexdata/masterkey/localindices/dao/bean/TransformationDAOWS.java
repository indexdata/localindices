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
 * @author jakub
 */
public class TransformationDAOWS implements TransformationDAO {

    private String serviceBaseURL;
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");

    public TransformationDAOWS(String serviceBaseURL) {
        this.serviceBaseURL = serviceBaseURL;
    }

    /**
     * Retrieve list of all storages from the Web Service
     * @return
     */
    @Override
    public List<TransformationBrief> retrieveTransformationBriefs(int start, int max) {
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
            logger.log(Level.DEBUG, male);
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
            ResourceConnector<TransformationConverter> storageConnector =
                    new ResourceConnector<TransformationConverter>(
                    href.getResourceUri().toURL(),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            return storageConnector.get().getEntity();
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
        return null;
    } // retrieveFromBrief

    /**
     * PUT Transformation to the Web Service
     * @param Transformation entity to be put
     */
    @Override
    public Transformation updateTransformation(Transformation transformation) {
        try {
            ResourceConnector<TransformationConverter> storageConnector =
                    new ResourceConnector<TransformationConverter>(
                    new URL(serviceBaseURL + transformation.getId() + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            TransformationConverter hc = new TransformationConverter();
            hc.setEntity(transformation);
            storageConnector.put(hc);
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
        return transformation	;
    } // updateJob

    @Override
    public void createTransformation(Transformation transformation) {
        try {
            ResourceConnector<TransformationsConverter> storagesConnector =
                    new ResourceConnector<TransformationsConverter>(
                    new URL(serviceBaseURL),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
        TransformationConverter storageContainer = new TransformationConverter();
        storageContainer.setEntity(transformation);
        storagesConnector.postAny(storageContainer);
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
    }

    @Override
    public Transformation retrieveTransformationById(Long id) {
    	Transformation hable = null;
        try {
            ResourceConnector<TransformationConverter> storageConnector =
                new ResourceConnector<TransformationConverter>(
                    new URL(serviceBaseURL + id + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            hable = storageConnector.get().getEntity();
        } catch (Exception male) {
            logger.log(Level.DEBUG,  male);
        }
        return hable;    
    }

    @Override
    public void deleteTransformation(Transformation transformation) {
        try {
            ResourceConnector<TransformationConverter> storageConnector =
                new ResourceConnector<TransformationConverter>(
                    new URL(serviceBaseURL + transformation.getId() + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            storageConnector.delete();
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
    }

    @Override
    public List<Transformation> retrieveTransformations(int start, int max) {
       //TODO this cannot be more stupid
       logger.log(Level.WARN, "This method id deprecetated and should not be used, use retrieveStorageBrief instead.");
       List<Transformation> hables = new ArrayList<Transformation>();
       List<TransformationBrief> hrefs = retrieveTransformationBriefs(start, max);
       if (hrefs != null) {
            for (TransformationBrief href : hrefs) {
            	Transformation hable = retrieveFromBrief(href);
                hables.add(hable);
            }
       }
       return hables;    
    }
    
    @Override
    public int getTransformationCount() {
        String url = serviceBaseURL + "?start=0&max=0";
        try {
            ResourceConnector<TransformationsConverter> rc =
                    new ResourceConnector<TransformationsConverter>(
                    new URL(url),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            TransformationsConverter tc = rc.get();
            return tc.getCount();
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
            return 0;
        }
        
    }
}
