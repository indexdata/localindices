/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.EntityQuery;
import com.indexdata.masterkey.localindices.dao.TransformationDAO;
import com.indexdata.masterkey.localindices.entity.BasicTransformation;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.entity.XmlTransformationStep;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationBrief;

/**
 *
 * @author jakub
 */
public class TransformationDAOFake implements TransformationDAO {
    private Map<Long, Transformation> transformations;
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");
    
    public TransformationDAOFake() {
    	transformations = new HashMap<Long, Transformation>();

    	Transformation transformation = new BasicTransformation();
		transformation.setId(new Long(1));
		transformation.setName("OAI(DC) to SOLR");
		TransformationStepDAOFake stepDao = new TransformationStepDAOFake();
		TransformationStep step = new XmlTransformationStep("OAI-RECORD", "Extract OAI-RECORD", "");
		stepDao.create(step);
		transformation.addStep(step,1);
		step = new XmlTransformationStep("OAI-DC to OAI/PZ", "Transform to OAI/PZ", "");
		stepDao.create(step);
		transformation.addStep(step,2);
		step = new XmlTransformationStep("PZ to SOLR", "Transform to SOLR", "");
		stepDao.create(step);
		Long pz2solrStep = step.getId();
		transformation.addStep(step,3);
		transformation.setEnabled(true);
		transformations.put(transformation.getId(), transformation);

		transformation = new BasicTransformation();
		transformation.setId(new Long(2));
		transformation.setName("OAI(MARCXML) to SOLR");
		step = new XmlTransformationStep("OAI(bulk) to OAI-RECORD", "Extract OAI-RECORD", "");
		stepDao.create(step);
		transformation.addStep(step, 1);
		step = new XmlTransformationStep("OAI-RECORD to PZ", "OAI-RECORD/DC to MARCXML", "");
		stepDao.create(step);
		transformation.addStep(step, 2);
		step = new XmlTransformationStep("MARCXML to OAI/PZ", "Transform to OAI/PZ", "");
		stepDao.create(step);
		transformation.addStep(step, 3);
		step = stepDao.retrieveById(pz2solrStep);
		transformation.addStep(step, 4);
		transformation.setEnabled(true);
		transformations.put(transformation.getId(), transformation);
    }

	@Override
    public List<TransformationBrief> retrieveBriefs(int start, int max, EntityQuery query) {
        List<TransformationBrief> srefs = new ArrayList<TransformationBrief>();
        for (Transformation transformation : transformations.values()) {
            TransformationBrief brief = new TransformationBrief(transformation);
			//sref.setResourceUri(new URI("http://localhost/harvestables/" + sref.getId() + "/)"));
			srefs.add(brief);
        }
        return srefs;
    }
	
    public Transformation retrieveFromBrief(TransformationBrief href) {
        try {
            return (Transformation) transformations.get(href.getId()).clone();
        } catch (CloneNotSupportedException cle) {
            logger.log(Level.DEBUG, cle);
        }
        return null;
    }

    public Transformation update(Transformation transformation) { 
        Transformation hclone = null;
        try {
            hclone = (Transformation) transformation.clone();
        } catch (CloneNotSupportedException cle) {
            logger.log(Level.DEBUG, cle);                    
        }
        transformations.put(hclone.getId(), hclone);
        return hclone;
    }

    public void create(Transformation transformation) {
    	long newId = 1; 
    	for (Long id : transformations.keySet()) {
    		if (id != null) 
    			if (newId <= id) 
    				newId = id + 1; 
    	}
    	transformation.setId(newId);
    	transformations.put(newId, transformation);
    }
    

    @Override
    public Transformation retrieveById(Long id) {
    	return transformations.get(id);
    }
    
    

    public Transformation updateTransformation(Transformation transformation, Transformation updTransformation) {
    	return transformations.put(updTransformation.getId(), updTransformation);
    }

    @Override
    public void delete(Transformation transformation) {
		transformations.remove(transformation.getId());
    }
    

    @Override
    public List<Transformation> retrieve(int start, int max, EntityQuery query) {
    	List<Transformation> list = new LinkedList<Transformation>();
    	/* TODO filter right records */
    	for (Transformation transform: transformations.values()) 
    		list.add(transform);
    	return list;
    }

    @Override
    public int getCount(EntityQuery query) {
    	return transformations.size();
    }

  @Override
  public List<Transformation> retrieve(int start, int max, String sortKey,
    boolean asc, EntityQuery query) {
    return retrieve(start, max, query);
  }

  @Override
  public List<TransformationBrief> retrieveBriefs(int start, int max,
    String sortKey, boolean asc, EntityQuery query) {
    return retrieveBriefs(start, max, sortKey, asc, query);
  }
}
