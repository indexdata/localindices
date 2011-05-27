package com.indexdata.masterkey.localindices.web.service.converter;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.indexdata.masterkey.localindices.entity.Transformation;

@XmlRootElement(name = "transformationBrief")
public class TransformationBrief implements Comparable<Object> {
    private Long id;
    private String name;
    private String description;

    private URI uri; 
    
    public TransformationBrief() {
    }

    	
    public TransformationBrief(Transformation entity) {
        setId(entity.getId());
        setName(entity.getName());
        entity.getDescription();
    }

    /* TODO Verify */ 
    public TransformationBrief(Transformation entity, URI uri, boolean isUriExtendable) {
        this(entity);
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

    
    /**
     * Returns the URI associated with this reference converter.
     *
     * @return the converted uri
     */
    @XmlAttribute(name = "uri")
    
    public URI getResourceUri() {
        return uri;
    }

    /**
     * Sets the URI for this reference converter.
     *
     * @param uri resource uri
     */
    public void setResourceUri(URI uri) {
        this.uri = uri;
    }


	@Override
	public int compareTo(Object o) {
        return this.getName().compareTo(((TransformationBrief)o).getName());
	}

    public boolean equals(Object brief) {
        if (brief instanceof TransformationBrief) {
            return (this.getName().equals(((TransformationBrief)brief).getName()));
        } else {
            return false;
        }             
    }
    
    public int hashCode() {
        return this.getName().hashCode();
    }

	
}
