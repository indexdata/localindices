package com.indexdata.masterkey.localindices.dao;

import java.util.List;

import com.indexdata.masterkey.localindices.entity.TransformationStepAssociation;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepAssociationBrief;

public interface TransformationStepAssociationDAO extends CommonDAO<TransformationStepAssociation, TransformationStepAssociationBrief>
{
    // Non-standard interface for speed. 
    public List<TransformationStepAssociation> retrieveByTransformationId(Long transformationId);
    public List<TransformationStepAssociation> retrieveByStepId(Long stepId);
    public int getStepCountByTransformationId(Long transformationId);
    public int getTransformationCountByStepId(Long stepId);

}
