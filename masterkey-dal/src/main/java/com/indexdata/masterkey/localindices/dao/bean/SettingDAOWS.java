/*
 * Copyright (c) 1995-2013, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.dao.bean;

import com.indexdata.masterkey.localindices.dao.EntityInUse;
import com.indexdata.masterkey.localindices.dao.SettingDAO;
import com.indexdata.masterkey.localindices.entity.Setting;
import com.indexdata.masterkey.localindices.web.service.converter.SettingConverter;
import com.indexdata.masterkey.localindices.web.service.converter.SettingsConverter;
import com.indexdata.rest.client.ResourceConnectionException;
import com.indexdata.rest.client.ResourceConnector;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author jakub
 */
public class SettingDAOWS extends CommonDAOWS implements SettingDAO {
  private static Logger logger = Logger.getLogger(
    "com.indexdata.masterkey.harvester.dao");

  public SettingDAOWS(String serviceBaseURL) {
    super(serviceBaseURL);
  }

  @Override
  public void create(Setting entity) {
    try {
      ResourceConnector<SettingConverter> connector =
        new ResourceConnector<SettingConverter>(
        new URL(serviceBaseURL),
        "com.indexdata.masterkey.localindices.entity"
        + ":com.indexdata.masterkey.localindices.web.service.converter");
      SettingConverter container = new SettingConverter();
      container.setEntity(entity);
      URL url = connector.postAny(container);
      entity.setId(extractId(url));
    } catch (Exception male) {
      logger.log(Level.DEBUG, male);
    }
  }

  /**
   * GET entity from the Web Service
   *
   * @param id of entity to be fetched
   */
  @Override
  public Setting retrieveById(Long id) {
    Setting entity = null;
    try {
      ResourceConnector<SettingConverter> connector =
        new ResourceConnector<SettingConverter>(
        new URL(serviceBaseURL + id + "/"),
        "com.indexdata.masterkey.localindices.entity"
        + ":com.indexdata.masterkey.localindices.web.service.converter");
      entity = connector.get().getEntity();
    } catch (Exception male) {
      logger.log(Level.DEBUG, male);
    }
    return entity;
  }

  /**
   * PUT entity to the Web Service
   *
   * @param entity entity to be put
   */
  @Override
  public Setting update(Setting entity) {
    try {
      ResourceConnector<SettingConverter> connector =
        new ResourceConnector<SettingConverter>(
        new URL(serviceBaseURL + entity.getId() + "/"),
        "com.indexdata.masterkey.localindices.entity"
        + ":com.indexdata.masterkey.localindices.web.service.converter");
      SettingConverter hc = new SettingConverter();
      hc.setEntity(entity);
      connector.put(hc);
    } catch (Exception male) {
      logger.log(Level.DEBUG, male);
    }
    return entity;
  } // updateJob

  @Override
  public void delete(Setting entity) throws EntityInUse {
    try {
      ResourceConnector<SettingConverter> connector =
        new ResourceConnector<SettingConverter>(
        new URL(serviceBaseURL + entity.getId() + "/"),
        "com.indexdata.masterkey.localindices.entity"
        + ":com.indexdata.masterkey.localindices.web.service.converter");
      connector.delete();
    } catch (ResourceConnectionException rce) {
      if (EntityInUse.ERROR_MESSAGE.equals(rce.getServerMessage())) {
        throw new EntityInUse("Setting is in use", rce);
      }
    } catch (MalformedURLException male) {
      logger.warn(male.getMessage(), male);
    }
  }

  @Override
  public List<Setting> retrieve(int start, int max) {
    try {
      ResourceConnector<SettingsConverter> connector =
        new ResourceConnector<SettingsConverter>(
        new URL(serviceBaseURL),
        "com.indexdata.masterkey.localindices.entity"
        + ":com.indexdata.masterkey.localindices.web.service.converter");
      SettingsConverter hc = connector.get();
      List<Setting> entities = new ArrayList<Setting>(hc.getCount());
      for (SettingConverter container : hc.getReferences()) {
        entities.add(container.getEntity());
      }
      return entities;
    } catch (Exception male) {
      logger.log(Level.DEBUG, male);
      return null;
    }
  }

  @Override
  public int getCount() {
    String url = serviceBaseURL + "?start=0&max=0";
    try {
      ResourceConnector<SettingsConverter> connector =
        new ResourceConnector<SettingsConverter>(
        new URL(url),
        "com.indexdata.masterkey.localindices.entity"
        + ":com.indexdata.masterkey.localindices.web.service.converter");
      SettingsConverter hc = connector.get();
      return hc.getCount();
    } catch (Exception male) {
      logger.log(Level.DEBUG, male);
      return 0;
    }

  }

  @Override
  public List<Setting> retrieve(int start, int max, String sortKey, boolean asc) {
    return retrieve(start, max);
  }

}
