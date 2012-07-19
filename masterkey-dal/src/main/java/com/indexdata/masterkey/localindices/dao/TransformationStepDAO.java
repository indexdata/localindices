package com.indexdata.masterkey.localindices.dao;

import java.util.List;

import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationStepBrief;

public interface TransformationStepDAO extends CommonDAO<TransformationStep, TransformationStepBrief> 
{
  	/* Retrieve a list of Transformations using step */ 
  	List<Transformation> getTransformations(TransformationStep step);
}
