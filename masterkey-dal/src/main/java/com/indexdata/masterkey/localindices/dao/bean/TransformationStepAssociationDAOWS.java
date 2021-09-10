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

import com.indexdata.masterkey.localindices.dao.EntityQuery;
import com.indexdata.masterkey.localindices.dao.TransformationStepAssociationDAO;
import com.indexdata.masterkey.localindices.entity.TransformationStepAssociation;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepAssociationBrief;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepAssociationConverter;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepAssociationsConverter;
import com.indexdata.rest.client.ResourceConnector;

/**
 *
 * @author Dennis
 */
public class TransformationStepAssociationDAOWS extends CommonDAOWS implements TransformationStepAssociationDAO {

    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");

    public TransformationStepAssociationDAOWS(String serviceBaseURL) {
        super(serviceBaseURL);
    }

    /**
     * create (POST) entity to the Web Service
	 * @param entity the TSA to create
     */
    @Override
    public void create(TransformationStepAssociation entity) {
        try {
            ResourceConnector<TransformationStepAssociationsConverter> connector =
                    new ResourceConnector<>(
                    new URL(serviceBaseURL),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
        TransformationStepAssociationConverter container = new TransformationStepAssociationConverter();
        container.setEntity(entity);
        URL url = connector.postAny(container);
        entity.setId(extractId(url));
        } catch (Exception male) {
            logger.log(Level.ERROR, male);
        }
    }

    @Override
    public TransformationStepAssociation retrieveById(Long id) {
    	TransformationStepAssociation entity = null;
        try {
            ResourceConnector<TransformationStepAssociationConverter> connector =
                new ResourceConnector<>(
                    new URL(serviceBaseURL + id + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            entity = connector.get().getEntity();
        } catch (Exception male) {
            logger.log(Level.ERROR,  male);
        }
        return entity;    
    }

    /**
     * PUT Transformation to the Web Service
     * @param entity Transformation to be put
     */
    @Override
    public TransformationStepAssociation update(TransformationStepAssociation entity) {
        try {
            ResourceConnector<TransformationStepAssociationConverter> connector =
                    new ResourceConnector<>(
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
    public void delete(TransformationStepAssociation entity) {
        try {
            ResourceConnector<TransformationStepAssociationConverter> connector =
                new ResourceConnector<>(
                    new URL(serviceBaseURL + entity.getId() + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            connector.delete();
        } catch (Exception male) {
            logger.log(Level.ERROR, male);
        }
    }

	@Override
	public List<TransformationStepAssociation> retrieve(int start, int max, EntityQuery query) {
        String url = serviceBaseURL + "?start=" + start + "&max=" + max + query.asUrlParameters();
        try {
            ResourceConnector<TransformationStepAssociationsConverter> connector =
                    new ResourceConnector<>(
                    new URL(url),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            // TransformationStepAssociationsConverter  hc = connector.get();
            // TODO implement convertion !!! 
            // hc.getReferences();
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
        return null;
	}

	@Override
	public List<TransformationStepAssociationBrief> retrieveBriefs(int start, int max, EntityQuery query) {
        String url = serviceBaseURL + "?start=" + start + "&max=" + max + query.asUrlParameters();
        try {
            ResourceConnector<TransformationStepAssociationsConverter> connector =
                    new ResourceConnector<>(
                    new URL(url),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            TransformationStepAssociationsConverter  hc = connector.get();
            return hc.getReferences();
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
        return null;
	}

	@Override
	public TransformationStepAssociation retrieveFromBrief(
			TransformationStepAssociationBrief brief) {
		return retrieveById(brief.getId());
	}

	@Override
	public int getCount(EntityQuery query) {
        String url = serviceBaseURL + "?start=0&max=0" + query.asUrlParameters();
        try {
            ResourceConnector<TransformationStepAssociationsConverter> connector =
                    new ResourceConnector<>(
                    new URL(url),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            TransformationStepAssociationsConverter  hc = connector.get();
            return hc.getCount();
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
        return -1;
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

  @Override
  public List<TransformationStepAssociation> retrieve(int start, int max,
    String sortKey, boolean asc, EntityQuery query) {
    return retrieve(start, max, query);
  }

  @Override
  public List<TransformationStepAssociationBrief> retrieveBriefs(int start,
    int max, String sortKey, boolean asc, EntityQuery query) {
    return retrieveBriefs(start, max, query);
  }

	
}
