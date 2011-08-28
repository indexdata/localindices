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
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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

    public void create(Harvestable entity) {
    	if (entity.getId() == null) {
    		entity.setId(new Random().nextLong());
    	}
    	harvestables.put(entity.getId(), entity);
    }

    public List<HarvestableBrief> retrieveBriefs(int start, int max) {
        List<HarvestableBrief> hrefs = new ArrayList<HarvestableBrief>();
        for (Harvestable hable : harvestables.values()) {
            try {
                HarvestableBrief href = new HarvestableBrief(hable);
                href.setResourceUri(new URI("http://localhost/records/harvestables/" + href.getId() + "/)"));
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

    public Harvestable update(Harvestable harvestable) { 
        Harvestable hclone = null;
        try {
            hclone = (Harvestable) harvestable.clone();
        } catch (CloneNotSupportedException cle) {
            logger.log(Level.DEBUG, cle);                    
        }
        harvestables.put(hclone.getId(), hclone);
        return hclone;
    }

    public Harvestable retrieveById(Long id) {
    	return harvestables.get(id);
    }

    public void delete(Harvestable harvestable) {
    	harvestables.remove(harvestable.getId());
    }

    public List<Harvestable> retrieve(int start, int max) {
    	List<Harvestable> list = new LinkedList<Harvestable>();
    	int index = 0; 
    	for (Harvestable entity : harvestables.values()) {
    		if (index >= start)
    			list.add(entity);
    		if (list.size() >= max)
    			break;
    		index++;
    	}
    	return list;
    }

    public int getCount() {
    	return harvestables.size();
    }

    @Override
    public InputStream getLog(long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
