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

import com.indexdata.masterkey.localindices.dao.TransformationDAO;
import com.indexdata.masterkey.localindices.entity.BasicTransformationStep;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
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

    	Transformation transformation = new Transformation() {
			private static final long serialVersionUID = 1L;
    	};
		transformation.setId(new Long(1));
		transformation.setName("OAI(DC) to SOLR");
		List<TransformationStep> steps = new LinkedList<TransformationStep>();
		steps.add(new BasicTransformationStep("OAI-RECORD", "Extract OAI-RECORD", ""));
		steps.add(new BasicTransformationStep("OAI-DC to OAI/PZ", "Transform to OAI/PZ", ""));
		steps.add(new BasicTransformationStep("PZ to SOLR", "Transform to SOLR", ""));
		transformation.setSteps(steps);
		transformation.setEnabled(true);
		transformations.put(transformation.getId(), transformation);

		transformation = new Transformation() {
			private static final long serialVersionUID = 1L;
    	};
		transformation.setId(new Long(2));
		transformation.setName("OAI(MARCXML) to SOLR");
		steps = new LinkedList<TransformationStep>();
		steps.add(new BasicTransformationStep("OAI(bulk) to OAI-RECORD", "Extract OAI-RECORD", ""));
		steps.add(new BasicTransformationStep("OAI-RECORD to PZ", "OAI-RECORD/DC to MARCXML", ""));
		steps.add(new BasicTransformationStep("MARCXML to OAI/PZ", "Transform to OAI/PZ", ""));
		steps.add(new BasicTransformationStep("PZ to SOLR", "Transform to SOLR", ""));
		transformation.setSteps(steps);
		transformation.setEnabled(true);
		transformations.put(transformation.getId(), transformation);
		
    }

	@Override
    public List<TransformationBrief> retrieveTransformationBriefs(int start, int max) {
        List<TransformationBrief> srefs = new ArrayList<TransformationBrief>();
        for (Transformation transformation : transformations.values()) {
            TransformationBrief brief = new TransformationBrief(transformation);
			//sref.setResourceUri(new URI("http://localhost/harvestables/" + sref.getId() + "/)"));
			srefs.add(brief);
        }
        return srefs;
    }
	
	/*
	public List<TransformationBrief> retrieveTranformationBriefs(int start, int max) {
		// TODO Auto-generated method stub
		return null;
	}
	*/

    public Transformation retrieveFromBrief(TransformationBrief href) {
        try {
            return (Transformation) transformations.get(href.getId()).clone();
        } catch (CloneNotSupportedException cle) {
            logger.log(Level.DEBUG, cle);
        }
        return null;
    }

    public Transformation updateTransformation(Transformation transformation) { 
        Transformation hclone = null;
        try {
            hclone = (Transformation) transformation.clone();
        } catch (CloneNotSupportedException cle) {
            logger.log(Level.DEBUG, cle);                    
        }
        transformations.put(hclone.getId(), hclone);
        return hclone;
    }

    public void createTransformation(Transformation transformation) {
    	long newId = 1; 
    	for (Long id : transformations.keySet()) {
    		if (id != null) 
    			if (newId <= id) 
    				newId = id + 1; 
    	}
    	transformation.setId(newId);
    	transformations.put(newId, transformation);
    }
    

    public Transformation retrieveTransformationById(Long id) {
    	return transformations.get(id);
    }
    
    

    public Transformation updateTransformation(Transformation transformation, Transformation updTransformation) {
    	return transformations.put(updTransformation.getId(), updTransformation);
    }

    public void deleteTransformation(Transformation transformation) {
    	transformations.remove(transformation);
    }

    public List<Transformation> retrieveTransformations(int start, int max) {
    	List<Transformation> list = new LinkedList<Transformation>();
    	/* TODO filter right records */
    	for (Transformation transform: transformations.values()) 
    		list.add(transform);
    	return list;
    }

    public int getTransformationCount() {
    	return transformations.size();
    }
}
