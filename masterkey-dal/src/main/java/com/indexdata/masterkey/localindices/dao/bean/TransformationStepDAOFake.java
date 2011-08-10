/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.TransformationStepDAO;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepBrief;

/**
 *
 * @author Dennis
 */
public class TransformationStepDAOFake implements TransformationStepDAO {
    private Map<Long, TransformationStep> steps;
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");
    //List<TransformationStep> steps; 
    
    public TransformationStepDAOFake() 
    {
    }

	@Override
    public List<TransformationStepBrief> retrieveBriefs(int start, int max) {
        List<TransformationStepBrief> srefs = new ArrayList<TransformationStepBrief>();
        for (TransformationStep transformation : steps.values()) {
        	TransformationStepBrief brief = new TransformationStepBrief(transformation);
        	// TODO implement filter
			srefs.add(brief);
        }
        return srefs;
    }
	
    public TransformationStep retrieveFromBrief(TransformationStepBrief href) {
        try {
            return (TransformationStep) steps.get(href.getId()).clone();
        } catch (CloneNotSupportedException cle) {
            logger.log(Level.DEBUG, cle);
        }
        return null;
    }

    public TransformationStep updateTransformationStep(TransformationStep transformation) { 
        TransformationStep hclone = null;
        try {
            hclone = (TransformationStep) transformation.clone();
        } catch (CloneNotSupportedException cle) {
            logger.log(Level.DEBUG, cle);                    
        }
        steps.put(hclone.getId(), hclone);
        return hclone;
    }

    public void createTransformationStep(TransformationStep transformation) {
    	long newId = 1; 
    	for (Long id : steps.keySet()) {
    		if (id != null) 
    			if (newId <= id) 
    				newId = id + 1; 
    	}
    	transformation.setId(newId);
    	steps.put(newId, transformation);
    }
    

    public TransformationStep retrieveTransformationById(Long id) {
    	return steps.get(id);
    }
    
    

    public TransformationStep update(TransformationStep transformation, TransformationStep updTransformation) {
    	return steps.put(updTransformation.getId(), updTransformation);
    }

    public void delete(TransformationStep transformation) {
    	steps.remove(transformation);
    }

    public List<TransformationStep> retrieve(int start, int max) {
    	List<TransformationStep> list = new LinkedList<TransformationStep>();
    	/* TODO filter right records */
    	for (TransformationStep transform: steps.values()) 
    		list.add(transform);
    	return list;
    }

    public int getCount() {
    	return steps.size();
    }

	@Override
	public void create(TransformationStep step) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public TransformationStep retrieveById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransformationStep update(TransformationStep ts) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	@Override
	public List<TransformationStep> retrieveByTransformationId(
			int transformationId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getTransformationCountByTransformationId(int transformationId) {
		// TODO Auto-generated method stub
		return 0;
	}
	*/
}
