/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.dao.bean;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.DAOException;
import com.indexdata.masterkey.localindices.dao.EntityQuery;
import com.indexdata.masterkey.localindices.dao.HarvestableDAO;
import com.indexdata.masterkey.localindices.entity.HarvestConnectorResource;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.web.service.converter.HarvestableBrief;

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
            hable.setScheduleString("* 1 * * *");
            hable.setUrl("http://heinonline.org/HOL/OAI");
            hable.setMetadataPrefix("oai_dc");
            hable.setEnabled(false);
            harvestables.put(hable.getId(), hable);
            
            OaiPmhResource hable2 = new OaiPmhResource();
            hable2.setId(new Long(2));
            hable2.setLastUpdated(new SimpleDateFormat("MM/dd/yy").parse("04/04/2008"));
            hable2.setName("University of Groningen");
            hable2.setServiceProvider("University Digital Archive of the University of Groningen, The Netherlands");
            hable2.setScheduleString("* 1 * * *");
            hable2.setUrl("http://ir.ub.rug.nl/oai/");
            hable2.setMetadataPrefix("oai_dc");
            hable2.setEnabled(true);
            harvestables.put(hable2.getId(), hable2);

            XmlBulkResource hable3 = new XmlBulkResource();
            hable3.setId(new Long(3));
            hable3.setLastUpdated(new SimpleDateFormat("MM/dd/yy").parse("04/04/2008"));
            hable3.setName("University of Groningen");
            hable3.setServiceProvider("University Digital Archive of the University of Groningen, The Netherlands");
            hable3.setScheduleString("* 1 * * *");
            hable3.setUrl("http://ir.ub.rug.nl/oai/");
            hable3.setOpenAccess(true);
            hable3.setEnabled(true);
            hable3.setJson("{ \"searchables.settings\" : { \"cclmapau\" : \"CCL_JSON_OVERRIDE\", \"facetmap_author\" : \"FACETMAP_JSON_OVERRIDE\"} } ");
            harvestables.put(hable3.getId(), hable3);

        
            HarvestConnectorResource hable4 = new HarvestConnectorResource();
            hable4.setId(new Long(4));
            hable4.setLastUpdated(new SimpleDateFormat("MM/dd/yy").parse("04/04/2008"));
            hable4.setName("University of Groningen");
            hable4.setServiceProvider("University Digital Archive of the University of Groningen, The Netherlands");
            hable4.setScheduleString("* 1 * * *");
            hable4.setRecordLimit(1000);
            hable4.setJson("{ \"searchables.prefixes\" : \"searchables.harvestconnector.,openaccess.searchables.\" }");
            hable4.setOpenAccess(true);
            hable4.setEnabled(true);
            harvestables.put(hable4.getId(), hable4);

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
    public InputStream getLog(long id, Date from) throws DAOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

  @Override
  public List<Harvestable> retrieve(int start, int max, String sortKey,
    boolean asc) {
    return retrieve(start, max);
  }

  @Override
  public List<HarvestableBrief> retrieveBriefs(int start, int max,
    String sortKey, boolean asc) {
    return retrieveBriefs(start, max);
  }

  @Override
  public InputStream reset(long id) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void resetCache(long id) throws DAOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public List<Harvestable> retrieve(int start, int max, String sortKey, boolean asc, EntityQuery query) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<HarvestableBrief> retrieveBriefs(int start, int max, String sortKey, boolean asc, EntityQuery query) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getCount(EntityQuery query) {
    // TODO Auto-generated method stub
    return 0;
  }
 
}
