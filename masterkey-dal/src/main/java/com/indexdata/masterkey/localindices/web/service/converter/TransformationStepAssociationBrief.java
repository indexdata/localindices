package com.indexdata.masterkey.localindices.web.service.converter;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.indexdata.masterkey.localindices.entity.TransformationStepAssociation;

@XmlRootElement(name = "tsaBrief")
public class TransformationStepAssociationBrief implements Comparable<Object> {
    private Long id;
    private Long transformationId; 
    private Long stepId; 
    private Long position; 
    
    private URI uri; 

    public TransformationStepAssociationBrief() {
    }

    public TransformationStepAssociationBrief(TransformationStepAssociation entity) {
        setId(entity.getId());
        setTransformationId(entity.getTransformationId());
        setStepId(entity.getStep().getId());
    }

    	
    public TransformationStepAssociationBrief(TransformationStepAssociation entity, URI uri, boolean isUriExtendable) {
    	this(entity);
    	if (isUriExtendable) {
            try {
                this.uri = new URI(uri.toString() + entity.getId() + "/");
            } catch (URISyntaxException urie) {              
            }
        }
        else 
        	this.uri = uri;
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
        int stepCompare = this.stepId.compareTo(o.stepId);
        if (stepCompare != 0)
        	return stepCompare;
        return this.getPosition().compareTo(o.getPosition());
	}

    public boolean equals(Object object) {
        if (object instanceof TransformationStepAssociationBrief) {
        	TransformationStepAssociationBrief brief = (TransformationStepAssociationBrief) object;
            return 
            	(this.getTransformationId().equals(brief.getTransformationId())) &&
            	(this.stepId.equals(brief.stepId));
        } else {
            return false;
        }             
    }
    
	public void setResourceUri(URI uri) {
		this.uri = uri;
	}


    /**
     * Returns the URI associated with this reference converter.
     *
     * @return the converted uri
     */
    @XmlAttribute(name = "uri")
	public URI getResourceUri() {
		return uri;
	}


	public void setTransformationId(Long transformationId) {
		this.transformationId = transformationId;
	}


	public Long getTransformationId() {
		return transformationId;
	}

	public void setStepId(Long step) {
		this.stepId = step;
	}

	public void setPosition(Long position) {
		this.position = position;
	}

	public Long getPosition() {
		return position;
	}
}
