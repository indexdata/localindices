package com.indexdata.masterkey.localindices.dao;

import java.util.List;

import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationBrief;

public interface TransformationDAO {
    public void createTransformation(Transformation transformation);
    public Transformation retrieveTransformationById(Long id);
    public Transformation updateTransformation(Transformation tf);
    public void deleteTransformation(Transformation storage);
    public List<Transformation> retrieveTransformations(int start, int max);
/*
    public InputStream getStorageLog(long id);
*/
    /**
     * Retrieve a list of brief (listing) storages.
     * @return
     */
    List<TransformationBrief> retrieveTransformationBriefs(int start, int max);
    /**
     * Retrieves a Storage using it's listing reference (brief)
     * @param hbrief brief (listing) Storage
     * @return Storage detailed 
     */
    Transformation retrieveFromBrief(TransformationBrief tbrief);
    
    int getTransformationCount();
	
	

}
