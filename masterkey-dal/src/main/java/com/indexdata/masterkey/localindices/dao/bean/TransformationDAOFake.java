/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.TransformationDAO;
import com.indexdata.masterkey.localindices.entity.Transformation;
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
		transformation.setName("Test Local Index");
		//transformation.setUrl("http://localhost:8983");
		
		transformations.put(transformation.getId(), transformation);
		
		/*
		Transformation transform2 ; // = new Transformation();
		transform2.setId(new Long(2));
		transform2.setName("University of Groningen");
//            transform2.setUrl("http://ir.ub.rug.nl/oai/");
		
		transformations.put(transform2.getId(), transform2);
		*/
    }

	@Override
    public List<TransformationBrief> retrieveTransformationBriefs(int start, int max) {
        List<TransformationBrief> srefs = new ArrayList<TransformationBrief>();
        for (Transformation storage : transformations.values()) {
            TransformationBrief sref = new TransformationBrief(storage);
			//sref.setResourceUri(new URI("http://localhost/harvestables/" + sref.getId() + "/)"));
			srefs.add(sref);
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

    public Transformation updateTransformation(Transformation storage) { 
        Transformation hclone = null;
        try {
            hclone = (Transformation) storage.clone();
        } catch (CloneNotSupportedException cle) {
            logger.log(Level.DEBUG, cle);                    
        }
        transformations.put(hclone.getId(), hclone);
        return hclone;
    }

    public void createTransformation(Transformation storage) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Transformation retrieveTransformationById(Long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Transformation updateTransformation(Transformation storage, Transformation updTransformation) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void deleteTransformation(Transformation storage) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<Transformation> retrieveTransformations(int start, int max) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getTransformationCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
