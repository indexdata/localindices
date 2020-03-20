/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.DAOException;
import com.indexdata.masterkey.localindices.dao.EntityQuery;
import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableConverter;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestablesConverter;
import com.indexdata.rest.client.ResourceConnector;
import com.indexdata.utils.DateUtil;
import com.indexdata.utils.TextUtils;

/**
 *
 * @author jakub
 */
public class HarvestableDAOWS extends CommonDAOWS implements HarvestableDAO {

    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");

    public HarvestableDAOWS(String serviceBaseURL) {
        super(serviceBaseURL);
    }

    /**
     * create (POST) entity to the Web Service
	 * @param Harvestable
     * @return
     */
    @Override
    public void create(Harvestable harvestable) {
        try {
            ResourceConnector<HarvestablesConverter> harvestablesConnector =
                    new ResourceConnector<HarvestablesConverter>(
                    new URL(serviceBaseURL),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            HarvestableConverter harvestableContainer = new HarvestableConverter();
            harvestableContainer.setEntity(harvestable);
            URL url = harvestablesConnector.postAny(harvestableContainer);
	    	harvestable.setId(extractId(url));
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
    }

    /**
     * Retrieve (GET) entity from the Web Service
	 * @param id of the entity
     * @return Harvestable
     */
    @Override
    public Harvestable retrieveById(Long id) {
        Harvestable hable = null;
        try {
            ResourceConnector<HarvestableConverter> harvestableConnector =
                new ResourceConnector<HarvestableConverter>(
                    new URL(serviceBaseURL + id + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            hable = harvestableConnector.get().getEntity();
        } catch (Exception male) {
            logger.log(Level.DEBUG,  male);
        }
        return hable;
    }
    /**
     * Retrieve list of all harvestables from the Web Service
     * @return
     */
    @Override
    public List<HarvestableBrief> retrieveBriefs(int start, int max, String sortKey, boolean asc) {
        String url = serviceBaseURL + "?start=" + start + "&max=" + max;
        if (sortKey != null && !sortKey.isEmpty()) {
          try {
            url += "&sort="+ URLEncoder.encode((asc ? "" : "~") + sortKey, "UTF-8");
          } catch (UnsupportedEncodingException enc) {
            logger.error("Error encoding sort spec", enc);
          }
        }
        try {
            ResourceConnector<HarvestablesConverter> harvestablesConnector =
                    new ResourceConnector<HarvestablesConverter>(
                    new URL(url),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            HarvestablesConverter hc = harvestablesConnector.get();
            return hc.getReferences();
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
        return null;
    }


    /**
     * Retrieve harvestable from the Web Service using it's reference (URL)
     * @param href harvestableRef entity
     * @return harvestable entity
     */
    @Override
    public Harvestable retrieveFromBrief(HarvestableBrief href) {
        try {
            ResourceConnector<HarvestableConverter> harvestableConnector =
                    new ResourceConnector<HarvestableConverter>(
                    href.getResourceUri().toURL(),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            return harvestableConnector.get().getEntity();
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
        return null;
    } // retrieveFromBrief

    /**
     * update (PUT) harvestable to the Web Service
     * @param harvestable entity to be put
     */
    @Override
    public Harvestable update(Harvestable harvestable) {
        try {
            ResourceConnector<HarvestableConverter> harvestableConnector =
                    new ResourceConnector<HarvestableConverter>(
                    new URL(serviceBaseURL + harvestable.getId() + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            HarvestableConverter hc = new HarvestableConverter();
            hc.setEntity(harvestable);
            harvestableConnector.put(hc);
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
        return harvestable;
    } // updateJob



    @Override
    public void delete(Harvestable harvestable) {
        try {
            ResourceConnector<HarvestableConverter> harvestableConnector =
                new ResourceConnector<HarvestableConverter>(
                    new URL(serviceBaseURL + harvestable.getId() + "/"),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            harvestableConnector.delete();
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
    }

    @Override
    public List<Harvestable> retrieve(int start, int max, String sortKey, boolean asc) {
       //TODO this cannot be more stupid
       logger.log(Level.WARN, "This method id deprecetated and should not be used, use retrieveHarvestableBrief instead.");
       List<Harvestable> hables = new ArrayList<Harvestable>();
       List<HarvestableBrief> hrefs = retrieveBriefs(start, max, sortKey, asc);
       if (hrefs != null) {
            for (HarvestableBrief href : hrefs) {
                Harvestable hable = retrieveFromBrief(href);
                hables.add(hable);
            }
       }
       return hables;
    }

    @Override
    public List<Harvestable> retrieve(int start, int max, String sortKey, boolean asc, EntityQuery query) {
      //TODO this cannot be more stupid
      logger.log(Level.WARN, "This method id deprecetated and should not be used, use retrieveHarvestableBrief instead.");
      List<Harvestable> hables = new ArrayList<Harvestable>();
      List<HarvestableBrief> hrefs = retrieveBriefs(start, max, sortKey, asc, query);
      if (hrefs != null) {
           for (HarvestableBrief href : hrefs) {
               Harvestable hable = retrieveFromBrief(href);
               hables.add(hable);
           }
      }
      return hables;
    }

    @Override
    public List<HarvestableBrief> retrieveBriefs(int start, int max, String sortKey, boolean asc, EntityQuery query) {
      String url = serviceBaseURL + "?start=" + start + "&max=" + max;
      if (sortKey != null && !sortKey.isEmpty()) {
        try {
          url += "&sort="+ URLEncoder.encode((asc ? "" : "~") + sortKey, "UTF-8");
        } catch (UnsupportedEncodingException enc) {
          logger.error("Error encoding sort spec", enc);
        }
      }
      url += query.asUrlParameters();
      try {
          ResourceConnector<HarvestablesConverter> harvestablesConnector =
                  new ResourceConnector<HarvestablesConverter>(
                  new URL(url),
                  "com.indexdata.masterkey.localindices.entity" +
                  ":com.indexdata.masterkey.localindices.web.service.converter");
          HarvestablesConverter hc = harvestablesConnector.get();
          return hc.getReferences();
      } catch (Exception male) {
          logger.log(Level.DEBUG, male);
      }
      return null;
    }

    @Override
    public int getCount() {
        String url = serviceBaseURL + "?start=0&max=0";
        try {
            ResourceConnector<HarvestablesConverter> harvestablesConnector =
                    new ResourceConnector<HarvestablesConverter>(
                    new URL(url),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
            HarvestablesConverter hc = harvestablesConnector.get();
            return hc.getCount();
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
            return 0;
        }

    }

    @Override
    public int getCount(EntityQuery query) {
      String url = serviceBaseURL + "?start=0&max=0" + query.asUrlParameters();
      try {
          ResourceConnector<HarvestablesConverter> harvestablesConnector =
                  new ResourceConnector<HarvestablesConverter>(
                  new URL(url),
                  "com.indexdata.masterkey.localindices.entity" +
                  ":com.indexdata.masterkey.localindices.web.service.converter");
          HarvestablesConverter hc = harvestablesConnector.get();
          return hc.getCount();
      } catch (Exception male) {
          logger.log(Level.DEBUG, male);
          return 0;
      }
    }


    @Override
    public InputStream getLog(long id, Date from) throws DAOException {
      String logURL = serviceBaseURL + id + "/" + "log/";
      if (from != null) {
        try {
          logURL += "?from=" +
            URLEncoder.encode(
              DateUtil.serialize(from, DateUtil.DateTimeFormat.ISO_EXT),
              "UTF-8");
        } catch (Exception e) {
          throw new DAOException("Cannot serialize 'from' argument", e);
        }
      }
      try {
        logger.debug("GET "+logURL);
        URL url = new URL(logURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int respCode = conn.getResponseCode();
        if (respCode == 204) {
          //no new entries
          return null;
        }
        return conn.getInputStream();
      } catch (FileNotFoundException fnf) {
        throw new DAOException("Job log not found", fnf);
      } catch (IOException ioe) {
        throw new DAOException("Error when contacting log webservice", ioe);
      }
    }

  @Override
  public List<Harvestable> retrieve(int start, int max) {
    return retrieve(start, max, null, true);
  }

  @Override
  public List<HarvestableBrief> retrieveBriefs(int start, int max) {
    return retrieveBriefs(start, max, null, true);
  }

  @Override
  public InputStream reset(long id) {
      String logURL = serviceBaseURL + id + "/" + "reset/";
      try {
          URL url = new URL(logURL);
          HttpURLConnection conn = (HttpURLConnection) url.openConnection();
          conn.setRequestMethod("GET");
          return conn.getInputStream();
      } catch (IOException ioe) {
          logger.log(Level.DEBUG, ioe);
          return null;
      }
  }

  @Override
  public void resetCache(long id) throws DAOException {
    String resetUrl = TextUtils.joinPath(serviceBaseURL, Long.toString(id), "cache/");
    try {
      URL url = new URL(resetUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("DELETE");
      int resp = conn.getResponseCode();
      logger.log(Level.DEBUG, "DELETE " + resetUrl + " -- " + resp);
    } catch (FileNotFoundException fnf) {
      throw new DAOException("Job cache not found -- 404");
    } catch (IOException ioe) {
      throw new DAOException("Undefined error when removing the job cache");
    }
  }



}
