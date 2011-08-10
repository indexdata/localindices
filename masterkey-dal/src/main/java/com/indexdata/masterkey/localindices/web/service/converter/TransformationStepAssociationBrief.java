package com.indexdata.masterkey.localindices.web.service.converter;

import java.net.URI;

import javax.xml.bind.annotation.XmlRootElement;

import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.entity.TransformationStepAssociation;

@XmlRootElement(name = "transformationStepAssociationBrief")
public class TransformationStepAssociationBrief implements Comparable<Object> {
    private Long id;
    private Long transformationId; 
    private TransformationStep step; 
    private Long position; 
    
    private URI uri; 

    public TransformationStepAssociationBrief() {
    }

    	
    public TransformationStepAssociationBrief(TransformationStepAssociation entity) {
        setId(entity.getId());
        setTransformationId(entity.getTransformationId());
        setStep(entity.getStep());
    }

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	@Override
	public int compareTo(Object obj) {
		TransformationStepAssociationBrief o = (TransformationStepAssociationBrief) obj; 
        int transformCompare = this.getTransformationId().compareTo(o.getTransformationId());
        if (transformCompare != 0)
        	return transformCompare;
        int stepCompare = this.getStep().getId().compareTo(o.getStep().getId());
        if (stepCompare != 0)
        	return stepCompare;
        return this.getPosition().compareTo(o.getPosition());
	}

    public boolean equals(Object object) {
        if (object instanceof TransformationStepAssociationBrief) {
        	TransformationStepAssociationBrief brief = (TransformationStepAssociationBrief) object;
            return 
            	(this.getTransformationId().equals(brief.getTransformationId())) &&
            	(this.getStep().getId().equals(brief.getStep().getId()));
        } else {
            return false;
        }             
    }
    
	public void setResourceUri(URI uri) {
		this.uri = uri;
	}


	public URI getResourceUri() {
		return uri;
	}


	public void setTransformationId(Long transformationId) {
		this.transformationId = transformationId;
	}


	public Long getTransformationId() {
		return transformationId;
	}

	public void setStep(TransformationStep step) {
		this.step = step;
	}


	public TransformationStep getStep() {
		return step;
	}


	public void setPosition(Long position) {
		this.position = position;
	}


	public Long getPosition() {
		return position;
	}
}
