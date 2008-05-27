/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.localindexes.scheduler.dao.bean;

import com.indexdata.localindexes.scheduler.dao.HarvestableDAOObsolete;
import com.indexdata.localindexes.web.entity.Harvestable;
import com.indexdata.localindexes.web.entity.OaiPmhResource;
import com.indexdata.localindexes.web.service.converter.HarvestableRefConverter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jakub
 */
public class HarvestableDAOFake implements HarvestableDAOObsolete {
    private Map<Long, Harvestable> harvestables;
    private static Logger logger = Logger.getLogger("com.indexdata.localindexes.scheduler.dao.bean");
    
    public HarvestableDAOFake() {
        harvestables = new HashMap<Long, Harvestable>();
        try {
            OaiPmhResource hable = new OaiPmhResource();
            hable.setId(new Long(1));
            hable.setLastUpdated(new SimpleDateFormat("MM/dd/yy").parse("05/05/2008"));
            hable.setName("HeinOnline");
            hable.setTitle("HeinOnline.org");
            hable.setDescription("leading preservation publisher producing long out-of-print legal research materials in reprint and microfilm/fiche format and also became the world's largest distributor of legal periodicals");
            hable.setScheduleString("* * * * *");
            hable.setUrl("http://heinonline.org/HOL/OAI");
            hable.setMetadataPrefix("oai_dc");
            
            harvestables.put(hable.getId(), hable);
            
            OaiPmhResource hable2 = new OaiPmhResource();
            hable2.setId(new Long(2));
            hable2.setLastUpdated(new SimpleDateFormat("MM/dd/yy").parse("04/04/2008"));
            hable2.setName("University of Groningen");
            hable2.setTitle("University Digital Archive of the University of Groningen, The Netherlands");
            hable2.setScheduleString("* * * * *");
            hable2.setUrl("http://ir.ub.rug.nl/oai/");
            hable2.setMetadataPrefix("oai_dc");
            
            harvestables.put(hable2.getId(), hable2);
            
        } catch (ParseException pe) {
            logger.log(Level.SEVERE, "This will never happen.");
        }
    }

    public Collection<HarvestableRefConverter> pollHarvestableRefList() {
        Collection<HarvestableRefConverter> hrefs = new ArrayList<HarvestableRefConverter>();
        for (Harvestable hable : harvestables.values()) {
            try {
                // update the date so it looks like the settings has been changed
                /*
                if (hable.getId() == 2) {
                    hable.setLastUpdated(new Date());
                    harvestables.put(hable.getId(), hable);
                }
                */
                HarvestableRefConverter href = new HarvestableRefConverter(hable);
                href.setResourceUri(new URI("http://localhost/harvestables/" + href.getId() + "/)"));
                hrefs.add(href);
            } catch (URISyntaxException urie) {
                logger.log(Level.SEVERE, "This will never happen.");
            }
        }
        return hrefs;
    }

    public Harvestable retrieveFromRef(HarvestableRefConverter href) {
        try {
            return (Harvestable) harvestables.get(href.getId()).clone();
        } catch (CloneNotSupportedException cle) {
            logger.log(Level.SEVERE, "This should never happen", cle);
        }
        return null;
    }

    public void updateHarvestable(Harvestable harvestable) { 
        Harvestable hclone = null;
        try {
            hclone = (Harvestable) harvestable.clone();
        } catch (CloneNotSupportedException cle) {
            logger.log(Level.SEVERE, "This should never happen");                    
        }
        harvestables.put(hclone.getId(), hclone);
        logger.log(Level.INFO, "harvestable updated");
    }
}
