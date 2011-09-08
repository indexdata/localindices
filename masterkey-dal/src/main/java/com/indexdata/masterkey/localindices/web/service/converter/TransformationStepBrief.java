package com.indexdata.masterkey.localindices.web.service.converter;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.indexdata.masterkey.localindices.entity.TransformationStep;

@XmlRootElement(name = "transformationStepBrief")
public class TransformationStepBrief implements Comparable<Object> {
    private Long id;
    private String name;
    private String description;
    
    private URI uri; 

    public TransformationStepBrief() {
    }

    	
    public TransformationStepBrief(TransformationStep entity, URI uri, boolean isUriExtendable) {
        setId(entity.getId());
        setName(entity.getName());
        setDescription(entity.getDescription());
        if (isUriExtendable) {
            try {
                this.uri = new URI(uri.toString() + entity.getId() + "/");
            } catch (URISyntaxException urie) {              
            }
        }
        
    }

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public int compareTo(Object o) {
        return this.getName().compareTo(((TransformationStepBrief)o).getName());
	}

    public boolean equals(Object brief) {
        if (brief instanceof TransformationStepBrief) {
            return (this.getName().equals(((TransformationStepBrief)brief).getName()));
        } else {
            return false;
        }             
    }
    
    public int hashCode() {
        return this.getName().hashCode();
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
}
