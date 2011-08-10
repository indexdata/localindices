/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.TransformationStepAssociationDAO;
import com.indexdata.masterkey.localindices.entity.TransformationStepAssociation;

/**
 *
 * @author Dennis
 */
public class TransformationStepAssociationDAOFake implements TransformationStepAssociationDAO {
    private Map<Long, TransformationStepAssociation> transformationStepAssociations;
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");
    //List<TransformationStep> steps; 
    
    public TransformationStepAssociationDAOFake() 
    {
    }

    public TransformationStepAssociation update(TransformationStepAssociation entity) { 
    	TransformationStepAssociation hclone = null;
        try {
            hclone = (TransformationStepAssociation) entity.clone();
        } catch (CloneNotSupportedException cle) {
            logger.log(Level.DEBUG, cle);                    
        }
        transformationStepAssociations.put(hclone.getId(), hclone);
        return hclone;
    }

	@Override
    public void create(TransformationStepAssociation entity) {
    	long newId = 1; 
    	for (Long id : transformationStepAssociations.keySet()) {
    		if (id != null) 
    			if (newId <= id) 
    				newId = id + 1; 
    	}
    	entity.setId(newId);
    	transformationStepAssociations.put(newId, entity);
    }
    
    public TransformationStepAssociation retrieveTransformationById(Long id) {
    	return transformationStepAssociations.get(id);
    }
    
    public void delete(TransformationStepAssociation entity) {
    	transformationStepAssociations.remove(entity.getId());
    }

	@Override
    public List<TransformationStepAssociation> retrieveByTransformationId(Long id) {
    	List<TransformationStepAssociation> list = new LinkedList<TransformationStepAssociation>();
    	/* TODO filter right records */
    	for (TransformationStepAssociation transform: transformationStepAssociations.values()) {
    		if (transform.getTransformationId() == id)
    			list.add(transform);
    	}
    	return list;
    }

	@Override
    public List<TransformationStepAssociation> retrieveByStepId(Long id) {
    	List<TransformationStepAssociation> list = new LinkedList<TransformationStepAssociation>();
    	/* TODO filter right records */
    	for (TransformationStepAssociation transform: transformationStepAssociations.values()) 
    		if (transform.getStepId() == id)
    		list.add(transform);
    	return list;
    }

    public int getCount() {
    	return transformationStepAssociations.size();
    }

	@Override
	public TransformationStepAssociation retrieveById(Long id) {
		return transformationStepAssociations.get(id);
	}

	@Override
	public int getStepCountByTransformationId(Long id) {
    	int count = 0;
		for (TransformationStepAssociation transform: transformationStepAssociations.values()) 
    		if (transform.getTransformationId() == id)
    			count++;
		return count;
	}

	@Override
	public int getTransformationCountByStepId(Long id) {
    	int count = 0;
		for (TransformationStepAssociation transform: transformationStepAssociations.values()) 
    		if (transform.getStepId() == id)
    			count++;
		return count;
	}

}
