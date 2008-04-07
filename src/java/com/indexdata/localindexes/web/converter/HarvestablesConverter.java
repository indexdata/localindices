/*
 * HarvestablesConverter
 *
 * Created on April 4, 2008, 12:06 PM
 *
 */

package com.indexdata.localindexes.web.converter;

import com.indexdata.localindexes.web.entitybeans.Harvestable;
import java.net.URI;
import java.util.Collection;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlAttribute;
import java.util.ArrayList;


/**
 *
 * @author jakub
 */

@XmlRootElement(name = "harvestables")
public class HarvestablesConverter {
    private Collection<Harvestable> entities;
    private Collection<HarvestableRefConverter> references;
    private URI uri;
    
    /** Creates a new instance of HarvestablesConverter */
    public HarvestablesConverter() {
    }

    /**
     * Creates a new instance of HarvestablesConverter.
     *
     * @param entities associated entities
     * @param uri associated uri
     */
    public HarvestablesConverter(Collection<Harvestable> entities, URI uri) {
        this.entities = entities;
        this.uri = uri;
    }

    /**
     * Returns a collection of HarvestableRefConverter.
     *
     * @return a collection of HarvestableRefConverter
     */
    @XmlElement(name = "harvestableRef")
    public Collection<HarvestableRefConverter> getReferences() {
        references = new ArrayList<HarvestableRefConverter>();
        if (entities != null) {
            for (Harvestable entity : entities) {
                references.add(new HarvestableRefConverter(entity, uri, true));
            }
        }
        return references;
    }

    /**
     * Sets a collection of HarvestableRefConverter.
     *
     * @param a collection of HarvestableRefConverter to set
     */
    public void setReferences(Collection<HarvestableRefConverter> references) {
        this.references = references;
    }

    /**
     * Returns the URI associated with this converter.
     *
     * @return the uri
     */
    @XmlAttribute(name = "uri")
    public URI getResourceUri() {
        return uri;
    }

    /**
     * Returns a collection Harvestable entities.
     *
     * @return a collection of Harvestable entities
     */
    @XmlTransient
    public Collection<Harvestable> getEntities() {
        entities = new ArrayList<Harvestable>();
        if (references != null) {
            for (HarvestableRefConverter ref : references) {
                entities.add(ref.getEntity());
            }
        }
        return entities;
    }
}
