package com.indexdata.masterkey.localindices.dao;

import java.util.List;

import com.indexdata.masterkey.localindices.entity.TransformationStepAssociation;

public interface TransformationStepAssociationDAO {
    public void create(TransformationStepAssociation entity);
    public TransformationStepAssociation retrieveById(Long id);
    public TransformationStepAssociation update(TransformationStepAssociation entity);
    public void delete(TransformationStepAssociation entity);
    
    public List<TransformationStepAssociation> retrieveByTransformationId(Long transformationId);
    public List<TransformationStepAssociation> retrieveByStepId(Long stepId);
    
    /**
     * Retrieve a list of brief (listing) storages.
     * @return
     */
    // List<TransformationStepBrief> retrieveBriefs(int transformationId);
    /**
     * Retrieves a Storage using it's listing reference (brief)
     * @param hbrief brief (listing) Storage
     * @return TransformationStep detailed 
     */
    /* TransformationStepAssociation retrieveFromBrief(TransformationStepBrief entity); */
    
    public int getStepCountByTransformationId(Long transformationId);
	public int getTransformationCountByStepId(Long stepId);


}
