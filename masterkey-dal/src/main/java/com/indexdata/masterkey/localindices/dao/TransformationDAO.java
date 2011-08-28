package com.indexdata.masterkey.localindices.dao;

import java.util.List;

import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationBrief;

public interface TransformationDAO extends CommonDAO<Transformation, TransformationBrief> {
    public void create(Transformation entity);
    public Transformation retrieveById(Long id);
    public Transformation update(Transformation entity);
    public void delete(Transformation entity);
    // Is this redudant? 
    public List<Transformation> retrieve(int start, int max);

    /**
     * Retrieve a list of brief (listing) storages.
     * @return
     */
    List<TransformationBrief> retrieveBriefs(int start, int max);
    /**
     * Retrieves a Storage using it's listing reference (brief)
     * @param hbrief brief (listing) Storage
     * @return Storage detailed 
     */
    Transformation retrieveFromBrief(TransformationBrief tbrief);
    
    int getCount();
	
	

}
