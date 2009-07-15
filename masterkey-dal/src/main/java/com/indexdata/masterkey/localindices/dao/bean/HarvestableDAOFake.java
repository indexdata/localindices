/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author jakub
 */
public class HarvestableDAOFake implements HarvestableDAO {
    private Map<Long, Harvestable> harvestables;
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester.dao");
    
    public HarvestableDAOFake() {
        harvestables = new HashMap<Long, Harvestable>();
        try {
            OaiPmhResource hable = new OaiPmhResource();
            hable.setId(new Long(1));
            hable.setLastUpdated(new SimpleDateFormat("MM/dd/yy").parse("05/05/2008"));
            hable.setName("HeinOnline");
            hable.setServiceProvider("HeinOnline.org");
            hable.setTechnicalNotes("leading preservation publisher producing long out-of-print legal research materials in reprint and microfilm/fiche format and also became the world's largest distributor of legal periodicals");
            hable.setScheduleString("* * * * *");
            hable.setUrl("http://heinonline.org/HOL/OAI");
            hable.setMetadataPrefix("oai_dc");
            
            harvestables.put(hable.getId(), hable);
            
            OaiPmhResource hable2 = new OaiPmhResource();
            hable2.setId(new Long(2));
            hable2.setLastUpdated(new SimpleDateFormat("MM/dd/yy").parse("04/04/2008"));
            hable2.setName("University of Groningen");
            hable2.setServiceProvider("University Digital Archive of the University of Groningen, The Netherlands");
            hable2.setScheduleString("* * * * *");
            hable2.setUrl("http://ir.ub.rug.nl/oai/");
            hable2.setMetadataPrefix("oai_dc");
            
            harvestables.put(hable2.getId(), hable2);
            
        } catch (ParseException pe) {
            logger.log(Level.DEBUG, pe);
        }
    }

    public List<HarvestableBrief> retrieveHarvestableBriefs(int start, int max) {
        List<HarvestableBrief> hrefs = new ArrayList<HarvestableBrief>();
        for (Harvestable hable : harvestables.values()) {
            try {
                // update the date so it looks like the settings has been changed
                /*
                if (hable.getId() == 2) {
                    hable.setLastUpdated(new Date());
                    harvestables.put(hable.getId(), hable);
                }
                */
                HarvestableBrief href = new HarvestableBrief(hable);
                href.setResourceUri(new URI("http://localhost/harvestables/" + href.getId() + "/)"));
                hrefs.add(href);
            } catch (URISyntaxException urie) {
                logger.log(Level.DEBUG, urie);
            }
        }
        return hrefs;
    }

    public Harvestable retrieveFromBrief(HarvestableBrief href) {
        try {
            return (Harvestable) harvestables.get(href.getId()).clone();
        } catch (CloneNotSupportedException cle) {
            logger.log(Level.DEBUG, cle);
        }
        return null;
    }

    public Harvestable updateHarvestable(Harvestable harvestable) { 
        Harvestable hclone = null;
        try {
            hclone = (Harvestable) harvestable.clone();
        } catch (CloneNotSupportedException cle) {
            logger.log(Level.DEBUG, cle);                    
        }
        harvestables.put(hclone.getId(), hclone);
        return hclone;
    }

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

    public List<Harvestable> retrieveHarvestables(int start, int max) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getHarvestableCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
