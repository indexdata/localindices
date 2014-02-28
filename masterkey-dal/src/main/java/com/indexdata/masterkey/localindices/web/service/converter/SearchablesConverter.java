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
import com.indexdata.torus.layer.KeyValue;
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
            layer.setId(entity.getId().toString());
            layer.setLayerName("final");
            layer.setName(entity.getName());
            layer.setServiceProvider(entity.getServiceProvider());
            
            if (entity.isOpenAccess()) {
              layer.setOpenAccess(entity.isOpenAccess() ? "1" : null);
              layer.getDynamicElements().add(new KeyValue("categories", "id_openaccess"));
            }
            Storage storage = entity.getStorage();
            if (storage instanceof SolrStorageEntity) {
            	Storage solrStorage = (Storage) storage;
            	// Ensure unique zurl
            	// TODO FIX URL when all instances of pazpar2 (1.6.38) and metaproxy(?) has been upgraded to use yaz 5.0.12.
            	// Holding back for now to be sure. 
            	layer.setZurl(modifySolrUrl(solrStorage.getSearchUrl()) + "#" + entity.getId());
		// layer.setZurl(appendQuery(appendSelect(solrStorage.getSearchUrl()), "fq=database:" + entity.getId()));
            	// layer.setExtraArgs();
            	layer.setExtraArgs("fq=database:" + entity.getId());
            	layer.setUdb("solr-" + entity.getId());
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
            	// TODO use new method: 
            	// layer.add/setElement(key, value);
            	// ONLY for elements that is not defined with a method! 
            	/* Author example
              	layer.setElement("facetmap_author", "author_exact");
		layer.setElement("limitmap_author", "rpn: @attr 1=author_exact @attr 3=6 ");
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
            if (entity.getOriginalUri() != null)
              layer.getDynamicElements().add(new KeyValue("originalUri", entity.getOriginalUri()));
            if (entity.getJson() != null)
              layer.getDynamicElements().add(new KeyValue("json", entity.getJson()));
            layers.add(layer);
            record.setLayers(layers);
            records.add(record);
        }
        super.setRecords(records);
        super.setUri(uri);
    }

    final String http = "http://";
    String select = "select";
    String slash = "/";

    private String modifySolrUrl(String url) {
      String zurl = url;
      // Yaz did not handled zurls with http://. 5.0.12 does
      if (zurl.startsWith(http))
        zurl = zurl.substring(http.length());
  
      // Also handled by yaz 5.0.12
      if (!zurl.endsWith(select)) {
        if (!zurl.endsWith(slash))
  	zurl = zurl.concat(slash);
        zurl.concat(select);
      }
      return zurl;
    }

    @SuppressWarnings("unused")
    private String appendSelect(String searchUrl) {
      // TODO Check for already having select in the solr url ?

      if (searchUrl.endsWith("/")) 
	return searchUrl + "select";
      return searchUrl + "/select";
    }

    @SuppressWarnings("unused")
    private String appendQuery(String searchUrl, String string) {
      // First query part?
      if (searchUrl.indexOf("?") == -1) 
	return searchUrl + "?" + string;
      return searchUrl + "&" + string;
    }

}
