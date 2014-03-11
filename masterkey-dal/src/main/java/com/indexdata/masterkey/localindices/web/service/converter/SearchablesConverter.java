/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service.converter;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.indexdata.masterkey.localindices.dao.SettingDAO;
import com.indexdata.masterkey.localindices.dao.bean.SettingDAOJPA;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Setting;
import com.indexdata.masterkey.localindices.entity.SolrStorageEntity;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.torus.Layer;
import com.indexdata.torus.Record;
import com.indexdata.torus.Records;
import com.indexdata.torus.layer.KeyValue;
import com.indexdata.torus.layer.SearchableTypeLayer;

/**
 * Converter to TORUS records of type searchable.
 * 
 * @author jakub
 */
@SuppressWarnings("unchecked")
@XmlRootElement(name = "records")
public class SearchablesConverter extends Records {
  /**
   * Meant to be used only by JAXB.
   */

  private SettingDAO dao;
  static Logger logger = Logger.getLogger(SearchableTypeLayer.class);
  static Map<String, Method> methodMap = new HashMap<String, Method>();

  // Create a method map from key value (case insensitive) to method.
  // Unpredictable in case if more methods only differs in casing of course

  static {
    Class<SearchableTypeLayer> layer;
    try {
      layer = (Class<SearchableTypeLayer>) Class
	  .forName("com.indexdata.torus.layer.SearchableTypeLayer");
      Method[] methods = layer.getMethods();
      for (Method method : methods) {
	if (method.getName().startsWith("set")) {
	  Class<?>[] parameters = method.getParameterTypes();
	  if (parameters.length == 1 && parameters[0] == String.class)
	    methodMap.put(method.getName().substring(3).toLowerCase(), method);
	}
      }
    } catch (ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public SearchablesConverter() {
    dao = new SettingDAOJPA();
  }

  public SearchablesConverter(SettingDAO dao) {
    this.dao = dao;
  }

  private void overrideSetting(SearchableTypeLayer layerInstance, String key, String value) {
    try {
      Method method = methodMap.get(key.toLowerCase());
      // Work-around for fields that have XmlEntity overrides (like cclmap_XX) 
      // We want to be able to name the setting as the element name, not the java method name. 
      if (key.toLowerCase().contains("_")) { 
	key = key.replaceAll("_", "");
	if (methodMap.containsKey(key))
	  method = methodMap.get(key);
      }
      if (method != null)
	method.invoke(layerInstance, value);
      else {
	List<KeyValue> elements = layerInstance.getDynamicElements();
	if (elements == null) {
	  elements = new LinkedList<KeyValue>();
	  layerInstance.setDynamicElements(elements);
	}
	// Would be nice with some higher-level functions in Layer to do this setKeyValue / addKeyValue 
	for (KeyValue keyValue : elements) {
	  if (keyValue.getName().equals(key)) {
	    keyValue.setValue(value);
	    return ;
	  }
	}
	// Key not there, add
	// TODO Validate key: It becomes a XML element name.
	elements.add(new KeyValue(key, value));
      }
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("Unable to override setting using method " + key + " due to " + e.getMessage());
    }

  }

  public SearchablesConverter(Collection<Harvestable> entities, URI uri) {
    dao = new SettingDAOJPA();
    init(entities, uri);
  }
  /**
   * Creates a new instance of SearchablesConverter.
   * 
   * @param entities
   *          associated entities
   * @param uri
   *          associated uri
   */
  public SearchablesConverter(Collection<Harvestable> entities, URI uri, SettingDAO dao) {
    this.dao = dao; 
    init(entities, uri);
}
  
  private void init(Collection<Harvestable> entities, URI uri) {
    Collection<Record> records = new ArrayList<Record>();
    for (Harvestable entity : entities) {
      if (!entity.getEnabled())
	continue;
      Record record = new Record("searchable");
      List<Layer> layers = new ArrayList<Layer>();
      SearchableTypeLayer layer = new SearchableTypeLayer();

      // TODO id must include url to be unique. In a file-system safe way, since
      // the torus makes a filename out of it.
      // The later should be fixed in the torus.
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
	layer.setZurl(appendQuery(appendSelect(solrStorage.getSearchUrl()),
	    "fq=database:" + entity.getId()));
	layer.setExtraArgs("defType=lucene");
	// TODO Just like id the udb should be unique across all (possible)
	// harvesters.
	layer.setUdb("solr-" + entity.getId());
	// TODO make configurable
	// but it can be overridden in Torus admin
	layer.setTransform("solr-pz2.xsl");
	// TODO Default Solr CCL MAP
	layer.setCclMapTerm("1=text");
	layer.setCclMapTi("1=title");
	layer.setCclMapAu("1=author");
	layer.setCclMapSu("1=subject");
	layer.setCclMapJournalTitle("1=journal-title");
	layer.setCclMapIssn("1=issn");
	layer.setCclMapIsbn("1=isbn");
	layer.setSRU("solr");
	layer.setSruVersion("");
	// Override with dynamic stuff
	loadSolrSearchable(entity, layer);
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

  private void loadSolrSearchable(Harvestable entity, SearchableTypeLayer layer) {
    Map<String, Object> map = new HashMap<String, Object>();
    if (entity.getJson() != null) {
      JSONParser reader = new JSONParser();
      try {
	Object object = reader.parse(entity.getJson());
	if (object instanceof Map)
	  map = (Map<String, Object>) object;
      } catch (ParseException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
      }
    }

    String[] defaultPrefixes = { "searchables.settings.", "solr.searchables." };
    if (map.get("searchables.prefixes") != null)
      defaultPrefixes = map.get("searchables.prefixes").toString().split(",");
    List<String> settingsPrefixes = new LinkedList<String>();
    for (String prefix : defaultPrefixes) {
      prefix = prefix.trim();
      if (!prefix.endsWith("."))
	prefix = prefix + ".";
      settingsPrefixes.add(prefix);
    }
    
    settingsPrefixes.add("job." + entity.getId() + ".searchables.");

    for (String prefix: settingsPrefixes) {
      if (prefix != null) {
	List<Setting> settings = dao.retrieve(0, dao.getCount(prefix), prefix, false);
	for (Setting setting : settings) {
	  overrideSetting(layer, setting.getName().substring(prefix.length()), setting.getValue());
	}
      }
    }
    // Now override with direct JSON values
    String jsonSearchableSettings = "searchables.settings";
    if (map.get(jsonSearchableSettings) != null) {
      Object obj = map.get(jsonSearchableSettings);
      if (obj instanceof Map) {
	Map<String, Object> searchableSettings = (Map<String, Object>) obj;
	for (String key : searchableSettings.keySet()) {
	  overrideSetting(layer, key, searchableSettings.get(key).toString());
	}
      }
    }
  }

  final String http = "http://";
  String select = "select";
  String slash = "/";

  @SuppressWarnings("unused")
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

  private String appendSelect(String searchUrl) {
    // TODO Check for already having select in the solr url ?

    if (searchUrl.endsWith("/"))
      return searchUrl + "select";
    return searchUrl + "/select";
  }

  private String appendQuery(String searchUrl, String string) {
    // First query part?
    if (searchUrl.indexOf("?") == -1)
      return searchUrl + "?" + string;
    return searchUrl + "&" + string;
  }
}
