/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service.converter;

import com.indexdata.masterkey.localindices.entity.TransformationStepAssociation;
import java.net.URI;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAttribute;
import java.util.ArrayList;


/**
 *
 * @author Dennis
 */

@XmlRootElement(name = "tranformationStepAssociations")
public class TransformationStepAssociationsConverter {
    private List<TransformationStepAssociationBrief> references;
    private URI uri;
    private int count;
    
    /** Creates a new instance of StoragesConverter */
    public TransformationStepAssociationsConverter() {
    }

    /**
     * Creates a new instance of TransformationStepsConverter.
     *
     * @param entities associated entities
     * @param uri associated uri
     */
    public TransformationStepAssociationsConverter(List<TransformationStepAssociation> entities, URI uri, int count) {
        this.references = new ArrayList<TransformationStepAssociationBrief>();
        for (TransformationStepAssociation entity : entities) {
            references.add(new TransformationStepAssociationBrief(entity /* TODO fix, uri, true */));
        }
        this.uri = uri;
        this.count = count;
    }

    /**
     * Returns a collection of TransformationStepAssociationBrief.
     *
     * @return a collection of TransformationStepAssociationBrief
     */
    @XmlElement(name = "transformationStepAssociation")
    public List<TransformationStepAssociationBrief> getReferences() {
        return references;
    }

    /**
     * Sets a collection of TransformationStepAssociationBrief.
     *
     * @param references a collection of TransformationStepAssociationBrief to set
     */
    public void setReferences(List<TransformationStepAssociationBrief> references) {
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

    public void setResourceUri(URI uri) {
        this.uri = uri;
    }
    
    @XmlAttribute(name = "count")
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }    
}
