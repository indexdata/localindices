/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.web.service.client.ResourceConnector;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableConverter;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableRefConverter;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestablesConverter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

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
    public Collection<HarvestableRefConverter> pollHarvestableRefList() {
        try {
            ResourceConnector<HarvestablesConverter> harvestablesConnector =
                    new ResourceConnector<HarvestablesConverter>(
                    new URL(serviceBaseURL),
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
    public Harvestable retrieveFromRef(HarvestableRefConverter href) {
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
    } // retrieveFromRef


    /**
     * PUT harvestable to the Web Service
     * @param harvestable entity to be put
     */
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

    public Harvestable updateHarvestable(Harvestable harvestable, Harvestable updHarvestable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

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

    public Collection<Harvestable> retrieveHarvestables(int start, int max) {
       //TODO this cannot be more stupid
       Collection<Harvestable> hables = new ArrayList<Harvestable>();
       Collection<HarvestableRefConverter> hrefs = pollHarvestableRefList();
       if (hrefs != null) {
            for (HarvestableRefConverter href : hrefs) {
                Harvestable hable = retrieveFromRef(href);
                hables.add(hable);
            }
       }
       return hables;    
    }

    public int getHarvestableCount() {
        return 100;
    }
}
