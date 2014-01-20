/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service.converter;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.SolrStorageEntity;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.torus.Layer;
import com.indexdata.torus.Record;
import com.indexdata.torus.Records;
import com.indexdata.torus.layer.SearchableTypeLayer;


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
    public SearchablesConverter(Collection<Harvestable> entities, URI uri) {
        Collection<Record> records = new ArrayList<Record>();
        for (Harvestable entity : entities) {
            if (!entity.getEnabled()) 
            	continue;            
            Record record = new Record("searchable");
            List<Layer> layers = new ArrayList<Layer>();
            SearchableTypeLayer layer = new SearchableTypeLayer();
            layer.setId(uri + "#" + entity.getId());
            layer.setLayerName("final");
            layer.setName(entity.getName());
            layer.setServiceProvider(entity.getServiceProvider());
            layer.setOpenAccess(entity.isOpenAccess() ? "1" : null);
            Storage storage = entity.getStorage();
            
            if (storage instanceof SolrStorageEntity) {
            	Storage solrStorage = (Storage) storage;
            	layer.setZurl(modifySolrUrl(solrStorage.getSearchUrl()));
            	layer.setUdb("solr-" + entity.getId());
            	layer.setExtraArgs("fq=database:" + entity.getId());
            	// TODO make configurable
            	// but it can be overridden in Torus admin
            	layer.setTransform("solr-pz2.xsl");
            	// TODO Default Solr CCL MAP 
            	layer.setCclMapTerm("1=text");
            	layer.setCclMapTi(  "1=title");
            	layer.setCclMapAu(  "1=author");
            	layer.setCclMapSu(  "1=subject");
            	layer.setCclMapJournalTitle("1=journal-title");
            	layer.setCclMapIssn("1=issn");
            	layer.setCclMapIsbn("1=isbn");
            	layer.setSRU("solr");
            	layer.setSruVersion("");
            	// Missing a dynamic method as: 
            	//layer.addElement(key, value);
            	// TODO Default Solr FACET MAP and LIMIT MAP
            	//List<Object> elements = layer.getOtherElements();
            	/*
            	elements.add(new JAXBElement(
            	  new QName("","rootTag"),String.class,"foo bar"));
            	*/
            	//elements.add()
            	// TODO These settings should be configurable for the Storage?
            } else { 
            	// Zebra
            	// Extract zurlbase from Zebra Instance
            	// layer.setZurl(zurlBase + "/" + entity.getId());
            	Logger.getLogger(this.getClass()).warn("Zebra Index not fully implemented");
            	// zebra specific
                layer.setElementSet("pz2snippet");
            }
            layers.add(layer);
            record.setLayers(layers);
            records.add(record);
        }
        super.setRecords(records);
        super.setUri(uri);
    }

    final String http = "http://";
    private String modifySolrUrl(String url) {
      String zurl = url;
      // Pazpar2 does not handled zurls with http://
      if (zurl.startsWith(http)) 
    	  zurl = zurl.substring(http.length());
      return zurl;
    }
}
