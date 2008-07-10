/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service.converter;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.torus.Layer;
import com.indexdata.torus.Record;
import com.indexdata.torus.Records;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * Converter to TORUS records of type searchable.
 * @author jakub
 */
@XmlRootElement(name="records")
public class SearchablesConverter extends Records {
    /**
     * Meant to be used only by JAXB.
     */
    public SearchablesConverter() {
    }

    /**
     * Creates a new instance of SearchablesConverter.
     *
     * @param entities associated entities
     * @param uri associated uri
     */
    public SearchablesConverter(Collection<Harvestable> entities, URI uri, String zurlBase) {
        Collection<Record> records = new ArrayList<Record>();
        for (Harvestable entity : entities) {            
            Record record = new Record("searchable");
            List<Layer> layers = new ArrayList<Layer>();
            SearchableTypeLayer layer = new SearchableTypeLayer();
            layer.seLayertName("final");
            layer.setName(entity.getName());
            layer.setZurl(zurlBase + "/job" + entity.getId());
            layer.setTransform("oai_dc.xsl");
            layers.add(layer);
            record.setLayers(layers);
            records.add(record);
        }
        super.setRecords(records);
        super.setUri(uri);
    }
}
