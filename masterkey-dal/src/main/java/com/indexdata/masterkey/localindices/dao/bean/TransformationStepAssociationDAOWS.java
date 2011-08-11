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

import com.indexdata.masterkey.localindices.dao.TransformationStepAssociationDAO;
import com.indexdata.masterkey.localindices.entity.TransformationStepAssociation;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepAssociationConverter;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepAssociationsConverter;
import com.indexdata.rest.client.ResourceConnector;

/**
 *
 * @author Dennis
 */
public class TransformationStepAssociationDAOWS implements TransformationStepAssociationDAO {

    private String serviceBaseURL;
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");

    public TransformationStepAssociationDAOWS(String serviceBaseURL) {
        this.serviceBaseURL = serviceBaseURL;
    }

    /**
     * PUT Transformation to the Web Service
     * @param Transformation entity to be put
     */
    @Override
    public TransformationStepAssociation update(TransformationStepAssociation entity) {
        try {
            ResourceConnector<TransformationStepAssociationConverter> connector =
                    new ResourceConnector<TransformationStepAssociationConverter>(
                    new URL(serviceBaseURL + entity.getId() + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            TransformationStepAssociationConverter hc = new TransformationStepAssociationConverter();
            hc.setEntity(entity);
            connector.put(hc);
        } catch (Exception male) {
            logger.log(Level.ERROR, male);
        }
        return entity	;
    } // updateJob

    @Override
    public void create(TransformationStepAssociation entity) {
        try {
            ResourceConnector<TransformationStepAssociationsConverter> connector =
                    new ResourceConnector<TransformationStepAssociationsConverter>(
                    new URL(serviceBaseURL),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
        TransformationStepAssociationConverter container = new TransformationStepAssociationConverter();
        container.setEntity(entity);
        connector.postAny(container);
        } catch (Exception male) {
            logger.log(Level.ERROR, male);
        }
    }

    @Override
    public TransformationStepAssociation retrieveById(Long id) {
    	TransformationStepAssociation entity = null;
        try {
            ResourceConnector<TransformationStepAssociationConverter> connector =
                new ResourceConnector<TransformationStepAssociationConverter>(
                    new URL(serviceBaseURL + id + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            entity = connector.get().getEntity();
        } catch (Exception male) {
            logger.log(Level.ERROR,  male);
        }
        return entity;    
    }

    @Override
    public void delete(TransformationStepAssociation entity) {
        try {
            ResourceConnector<TransformationStepAssociationConverter> connector =
                new ResourceConnector<TransformationStepAssociationConverter>(
                    new URL(serviceBaseURL + entity.getId() + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            connector.delete();
        } catch (Exception male) {
            logger.log(Level.ERROR, male);
        }
    }

	@Override
	public List<TransformationStepAssociation> retrieveByTransformationId(Long transformationId) {
		return null;
	}

	@Override
	public List<TransformationStepAssociation> retrieveByStepId(Long stepId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getStepCountByTransformationId(Long transformationId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getTransformationCountByStepId(Long stepId) {
		// TODO Auto-generated method stub
		return 0;
	}

    
}
