package com.indexdata.masterkey.localindices.web.service.converter;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.indexdata.masterkey.localindices.entity.Storage;

@XmlRootElement(name = "storageBrief")
public class StorageBrief implements Comparable<Object> {
    private Long id;
    private String name;
    private String description;
    private boolean enabled;

    private URI uri; 
    
    public StorageBrief() {
    }

    	
    public StorageBrief(Storage entity) {
        setId(entity.getId());
        setName(entity.getName());
        setDescription(entity.getDescription());
        if (entity.getEnabled() != null && entity.getEnabled())
        	setEnabled(true);
        else 
        	setEnabled(false);
        	
    }

    /* TODO Verify */ 
    public StorageBrief(Storage entity, URI uri, boolean isUriExtendable) {
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
	public int compareTo(Object storage) {
        return this.getName().compareTo(((StorageBrief)storage).getName());
	}
    
    public boolean equals(Object storage) {
        if (storage instanceof StorageBrief) {
            return (this.getName().equals(((HarvestableBrief)storage).getName()));
        } else {
            return false;
        }             
    }

    public int hashCode() {
        return this.getName().hashCode();
    }


	public String getEnabledDisplay() {
        if (enabled)
        	return	"Yes";
        return "";
	}

	public boolean isEnabled() {
		return enabled;
	}


	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
