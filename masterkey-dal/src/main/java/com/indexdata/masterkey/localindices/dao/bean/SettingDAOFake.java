/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.indexdata.masterkey.localindices.dao.EntityQuery;
import com.indexdata.masterkey.localindices.dao.SettingDAO;
import com.indexdata.masterkey.localindices.entity.Setting;

/**
 *
 * @author Dennis
 */
public class SettingDAOFake implements SettingDAO {
  private Map<Long, Setting> settings;
  //private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");

  public SettingDAOFake() {
    settings = new HashMap<Long, Setting>();
    String[][] settingsArray =
      {
	{ "solr.searchables.cclmapTerm", "Term Attributes",   "1=text" },
	{ "solr.searchables.cclmapTi",   "Title Attributes",  "1=title" },
	{ "solr.searchables.cclmapAu",   "Author Attributes", "1=author" },
	{ "solr.searchables.cclmapSu",   "Subject Attributes","1=subject" },
	{ "solr.searchables.cclmapSu",   "Date Attributes",   "1=date r=r" },
	{ "solr.searchables.cclmapJournalTitle",
	  				 "JournalTitle Attributes",  "1=journal-title" },
	{ "solr.searchables.cclmapIssn", "ISSN Attributes",  "1=issn" },
	{ "solr.searchables.cclmapIsbn", "ISBN Attributes",  "1=isbn" },
	{ "solr.searchables.sru", "SRU",  "solr" },
	{ "solr.searchables.sruversion", "SRU/Solr version",  "" },
	{ "solr.searchables.facetmap_author",  "Facetmap Author",  "author_exact" },
	{ "solr.searchables.facetmap_subject", "Facetmap Subject", "subject_exact" },
	{ "solr.searchables.facetmap_medium",  "Facetmap Medium",  "medium_exact" },
	{ "solr.searchables.facetmap_date",    "Facetmap Date",  "date" },
	{ "solr.searchables.limitmap_author",  "Limitmap Author",  "rpn: @attr 1=author_exact @attr 6=3" },
	{ "solr.searchables.limitmap_subject", "Limitmap Subject", "rpn: @attr 1=subject_exact @attr 6=3" },
	{ "solr.searchables.limitmap_medium",  "Limitmap Medium",  "rpn: @attr 1=medium_exact @attr 6=3" },
	{ "solr.searchables.limitmap_date",    "Limitmap Date",    "rpn: @attr 1=date @attr 6=3" },
	{ "job.2.searchables.limitmap_date",   "Limitmap Date",    "JOB.2 OVERRIDE" }
      };
    for (String[] values : settingsArray) {
      Setting setting = new Setting();
      setting.setId(newSettingId());
      setting.setName(values[0]);
      setting.setLabel(values[1]);
      setting.setValue(values[2]);
      settings.put(setting.getId(), setting);
    }
  }

  synchronized private Long newSettingId() {
    long index = 1l;
    for (Setting setting : settings.values()) {
      if (index <= setting.getId()) {
	index = setting.getId() + 1l;
      }
    }
    return index;
  }

  public Setting update(Setting setting) {
    Setting hclone = null;
    hclone = new Setting();
    hclone.setId(setting.getId());
    hclone.setName(setting.getName());
    hclone.setValue(setting.getValue());
    if (hclone.getId() == null)
      hclone.setId(newSettingId());
    settings.put(hclone.getId(), hclone);
    return hclone;
  }

  @Override
  public void create(Setting setting) {
    if (setting.getId() == null)
      setting.setId(newSettingId());
    settings.put(setting.getId(), setting);
  }

  public Setting retrieveById(Long id) {
    return settings.get(id);
  }

  public Setting updateSetting(Setting setting, Setting updSetting) {
    return settings.put(setting.getId(), updSetting);
  }

  @Override
  public void delete(Setting setting) {
    settings.remove(setting.getId());
  }

  @Override
  public List<Setting> retrieve(int start, int max, EntityQuery query) {
    List<Setting> list = new LinkedList<Setting>();
    for (Setting setting : settings.values()) {
      if (setting.getName().startsWith(query.getStartsWith()))
	list.add(setting);
      if (list.size() >= max)
	break;
    }
    return list;
  }

  @Override
  public int getCount(EntityQuery query) {
    return settings.keySet().size();
  }

  @Override
  public List<Setting> retrieve(int start, int max, String sortKey, boolean asc, EntityQuery query) {
    return retrieve(start, max, query);
  }

}
