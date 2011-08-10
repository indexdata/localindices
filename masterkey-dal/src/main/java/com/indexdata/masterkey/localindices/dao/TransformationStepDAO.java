package com.indexdata.masterkey.localindices.dao;

import java.util.List;

import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepBrief;

public interface TransformationStepDAO {

	/* CRUD */ 
	public void create(TransformationStep entity);
    public TransformationStep retrieveById(Long id);
    public TransformationStep update(TransformationStep entity);
    public void delete(TransformationStep entity);
    List<TransformationStep> retrieve(int start, int max);

    /**
     * Retrieve a list of brief entities.
     * @return
     */
    List<TransformationStepBrief> retrieveBriefs(int start, int max);
    /**
     * Retrieves a Step using it's listing reference (brief)
     * @param hbrief brief (listing) Step
     * @return TransformationStep detailed 
     */
    TransformationStep retrieveFromBrief(TransformationStepBrief tbrief);
    
    int getCount();
}
