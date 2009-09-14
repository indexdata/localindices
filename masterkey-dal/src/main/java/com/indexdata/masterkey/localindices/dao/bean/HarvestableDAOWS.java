/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.rest.client.ResourceConnector;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableConverter;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestablesConverter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

/**
 *
 * @author jakub
 */
public class HarvestableDAOWS implements HarvestableDAO {

    private String serviceBaseURL;
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");

    public HarvestableDAOWS(String serviceBaseURL) {
        this.serviceBaseURL = serviceBaseURL;
    }

    /**
     * Retrieve list of all harvestables from the Web Service
     * @return
     */
    @Override
    public List<HarvestableBrief> retrieveHarvestableBriefs(int start, int max) {
        String url = serviceBaseURL + "?start=" + start + "&max=" + max;
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
     * @return harvesatble entity
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
     * PUT harvestable to the Web Service
     * @param harvestable entity to be put
     */
    @Override
    public Harvestable updateHarvestable(Harvestable harvestable) {
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
    public void createHarvestable(Harvestable harvestable) {
        try {
            ResourceConnector<HarvestablesConverter> harvestablesConnector =
                    new ResourceConnector<HarvestablesConverter>(
                    new URL(serviceBaseURL),
                    "com.indexdata.masterkey.localindices.entity" +
                    ":com.indexdata.masterkey.localindices.web.service.converter");
        HarvestableConverter harvestableContainer = new HarvestableConverter();
        harvestableContainer.setEntity(harvestable);
        harvestablesConnector.postAny(harvestableContainer);
        } catch (Exception male) {
            logger.log(Level.DEBUG, male);
        }
    }

    @Override
    public Harvestable retrieveHarvestableById(Long id) {
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

    @Override
    public void deleteHarvestable(Harvestable harvestable) {
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
    public List<Harvestable> retrieveHarvestables(int start, int max) {
       //TODO this cannot be more stupid
       logger.log(Level.WARN, "This method id deprecetated and should not be used, use retrieveHarvestableBrief instead.");
       List<Harvestable> hables = new ArrayList<Harvestable>();
       List<HarvestableBrief> hrefs = retrieveHarvestableBriefs(start, max);
       if (hrefs != null) {
            for (HarvestableBrief href : hrefs) {
                Harvestable hable = retrieveFromBrief(href);
                hables.add(hable);
            }
       }
       return hables;    
    }

    @Override
    public int getHarvestableCount() {
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
    public InputStream getHarvestableLog(long id) {
        String logURL = serviceBaseURL + id + "/" + "log/";
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
}
