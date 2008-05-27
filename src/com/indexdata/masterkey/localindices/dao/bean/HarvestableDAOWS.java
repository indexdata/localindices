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
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jakub
 */
public class HarvestableDAOWS implements HarvestableDAO {

    private String serviceBaseURL;
    private static Logger logger;

    public HarvestableDAOWS(String serviceBaseURL, Logger logger) {
        this.serviceBaseURL = serviceBaseURL;
        this.logger = logger;
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
                    "com.indexdata.localindexes.web.entity" +
                    ":com.indexdata.localindexes.web.service.converter");
            HarvestablesConverter hc = harvestablesConnector.get();
            return hc.getReferences();
        } catch (Exception male) {
            logger.log(Level.SEVERE, "Cannot retrieve the list of harvestables", male);
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
                    "com.indexdata.localindexes.web.entity" +
                    ":com.indexdata.localindexes.web.service.converter");
            return harvestableConnector.get().getEntity();
        } catch (Exception male) {
            logger.log(Level.SEVERE, "Cannot retreve harvestable from it's ref", male);
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
                    "com.indexdata.localindexes.web.entity" +
                    ":com.indexdata.localindexes.web.service.converter");
            HarvestableConverter hc = new HarvestableConverter();
            hc.setEntity(harvestable);
            harvestableConnector.put(hc);
        } catch (Exception male) {
            logger.log(Level.SEVERE, "Cannot update harvestable", male);
        }
        return harvestable;
    } // updateJob

    public void createHarvestable(Harvestable harvestable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Harvestable retrieveHarvestableById(Long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Harvestable updateHarvestable(Harvestable harvestable, Harvestable updHarvestable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void deleteHarvestable(Harvestable harvestable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<Harvestable> retrieveHarvestables(int start, int max) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getHarvestableCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
