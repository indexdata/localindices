package com.indexdata.masterkey.localindices.web.service.converter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import com.indexdata.masterkey.localindices.dao.EntityQuery;
import com.indexdata.masterkey.localindices.dao.bean.HarvestableDAOFake;
import com.indexdata.masterkey.localindices.dao.bean.SettingDAOFake;
import com.indexdata.masterkey.localindices.dao.bean.StorageDAOFake;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Setting;
import com.indexdata.torus.Layer;
import com.indexdata.torus.Record;
import com.indexdata.torus.layer.KeyValue;
import com.indexdata.torus.layer.SearchableTypeLayer;

import junit.framework.TestCase;

public class TestSearchableConverter extends TestCase {

  public void testSearchableConverter() throws URISyntaxException {

    HarvestableDAOFake dao = new HarvestableDAOFake();
    StorageDAOFake storageDAO = new StorageDAOFake();
    EntityQuery qry = new EntityQuery();
    for (Harvestable harvestable : dao.retrieve(0, dao.getCount(qry), qry)) {
      harvestable.setStorage(storageDAO.retrieveById(new Long(1)));
    }

    SettingDAOFake settingDAO = new SettingDAOFake();
    SearchablesConverter searchables = new SearchablesConverter(dao.retrieve(0, dao.getCount(qry), qry),
	new URI("http://localhost/records/searchables"), settingDAO);

    assertTrue(searchables.getRecords() != null);
    assertEquals(searchables.getRecords().size(), 3);
    // Need to match the number of harvestable in Fake.
    String[] ccl_author_override = { "1=author", "CCL_JSON_OVERRIDE", "1=author" };
    int index = 0;
    // "FACETMAP_JSON_OVERRIDE"
    for (Record record : searchables.getRecords()) {
      assertTrue(record.getLayers() != null);
      assertTrue(record.getLayers().size() == 1);
      List<Layer> layers = record.getLayers();
      Layer layer = layers.get(0);
      assertTrue(layer instanceof SearchableTypeLayer);
      SearchableTypeLayer searchableLayer = (SearchableTypeLayer) layer;
      assertTrue(searchableLayer.getSRU().equals("solr"));
      assertTrue(searchableLayer.getSruVersion().equals(""));
      assertTrue(searchableLayer.getSruVersion().equals(""));
      assertTrue(searchableLayer.getCclMapAu().equals(ccl_author_override[index]));
      String categories = searchableLayer.getCategories();
      if (categories != null) {
	assertTrue(categories.equals("id_openaccess"));
	assertTrue(searchableLayer.getOpenAccess().equals("1"));
      }
      List<KeyValue> dynamicElements = searchableLayer.getDynamicElements();

      String prefix = "solr.searchables.";
      EntityQuery solrQry = new EntityQuery().withStartsWith(prefix, "name");
      for (KeyValue keyValue : dynamicElements) {
	if (keyValue.getName().equals("facetmap_author")) {
	  String[] facetmap_author_override = { "author_exact", "FACETMAP_JSON_OVERRIDE",
	      "author_exact" };
	  assertEquals(facetmap_author_override[index], keyValue.getValue());
	} else {
	  List<Setting> foundSetting = settingDAO.retrieve(0, settingDAO.getCount(solrQry),
	      qry);
	  if (foundSetting.size() == 1)
	    assertEquals(foundSetting.get(0).getValue(), keyValue.getValue());
	}
	if (keyValue.getName().equals("limitmap_date")) {
	  if (layer.getId().equals("2"))
	    assertEquals("JOB.2 OVERRIDE", keyValue.getValue());
	  else
	    assertEquals("rpn: @attr 1=date @attr 6=3", keyValue.getValue());
	}
      }

      index++;
    }

  }

}
