/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import java.net.URL;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.TransformationStepDAO;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationConverter;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepBrief;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepConverter;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepsConverter;
import com.indexdata.rest.client.ResourceConnector;

/**
 *
 * @author Dennis
 */
public class TransformationStepDAOWS extends CommonDAOWS implements TransformationStepDAO {

    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");

    public TransformationStepDAOWS(String serviceBaseURL) {
        super(serviceBaseURL);
    }

    /**
     * create (POST) entity to the Web Service
	 * @param TransformationStep
     * @return
     */
    @Override
    public void create(TransformationStep transformation) {
        try {
            ResourceConnector<TransformationStepsConverter> connector =
                    new ResourceConnector<TransformationStepsConverter>(
                    new URL(serviceBaseURL),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
        TransformationStepConverter container = new TransformationStepConverter();
        container.setEntity(transformation);
        URL url = connector.postAny(container);
        transformation.setId(extractId(url));
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
    }

    /**
     * Retrieve (GET) entity from the Web Service
     * @param id of the entity
     * @return TransformationStepAssociation
     */
    @Override
    public TransformationStep retrieveById(Long id) {
    	TransformationStep entity = null;
        try {
            ResourceConnector<TransformationStepConverter> connector =
                new ResourceConnector<TransformationStepConverter>(
                    new URL(serviceBaseURL + id + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            entity = connector.get().getEntity();
        } catch (Exception male) {
            logger.log(Level.DEBUG,  male);
        }
        return entity;    
    }

    /**
     * Retrieve list of all Steps from the Web Service
     * @return
     */
    @Override
    public List<TransformationStepBrief> retrieveBriefs(int start, int max) {
        String url = serviceBaseURL + "?start=" + start + "&max=" + max;
        try {
            ResourceConnector<TransformationStepsConverter> connector =
                    new ResourceConnector<TransformationStepsConverter>(
                    new URL(url),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            TransformationStepsConverter hc = connector.get();
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
    public TransformationStep retrieveFromBrief(TransformationStepBrief href) {
        try {
            ResourceConnector<TransformationStepConverter> connector =
                    new ResourceConnector<TransformationStepConverter>(
                    href.getResourceUri().toURL(),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            return connector.get().getEntity();
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
    public TransformationStep update(TransformationStep step) {
        try {
            ResourceConnector<TransformationStepConverter> connector =
                    new ResourceConnector<TransformationStepConverter>(
                    new URL(serviceBaseURL + step.getId() + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            TransformationStepConverter hc = new TransformationStepConverter();
            hc.setEntity(step);
            connector.put(hc);
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
        return step	;
    } // updateJob


    @Override
    public void delete(TransformationStep step) {
        try {
            ResourceConnector<TransformationConverter> storageConnector =
                new ResourceConnector<TransformationConverter>(
                    new URL(serviceBaseURL + step.getId() + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            storageConnector.delete();
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
    }

    @Override
    public int getCount() {
        String url = serviceBaseURL + "?start=0&max=0";
        try {
            ResourceConnector<TransformationStepsConverter> rc =
                    new ResourceConnector<TransformationStepsConverter>(
                    new URL(url),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            TransformationStepsConverter tc = rc.get();
            return tc.getCount();
        } catch (Exception male) {
            logger.log(Level.ERROR, male);
            return 0;
        }
    }

	@Override
	public List<TransformationStep> retrieve(int start, int max) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Transformation> getTransformations(TransformationStep step) {
	  // TODO Auto-generated method stub
	  return null;
	}

	@Override
	public List<TransformationStep> getEnabledSteps() {
	  // TODO Auto-generated method stub
	  return null;
	}

  @Override
  public List<TransformationStep> retrieve(int start, int max, String sortKey,
    boolean asc) {
    return retrieve(start, max);
  }

  @Override
  public List<TransformationStepBrief> retrieveBriefs(int start, int max,
    String sortKey, boolean asc) {
    return retrieveBriefs(start, max);
  }
}
