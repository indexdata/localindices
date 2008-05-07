/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.localindexes.scheduler.dao.bean;

import com.indexdata.localindexes.scheduler.dao.HarvestableDAO;
import com.indexdata.localindexes.web.entity.Harvestable;
import com.indexdata.localindexes.web.entity.OaiPmhResource;
import com.indexdata.localindexes.web.service.converter.HarvestableRefConverter;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jakub
 */
public class HarvestableDAOFake implements HarvestableDAO {

    private static Logger logger = Logger.getLogger("com.indexdata.localindexes.scheduler.dao.bean");

    public Collection<HarvestableRefConverter> pollHarvestableRefList() {
        Collection<HarvestableRefConverter> hrefs = new ArrayList<HarvestableRefConverter>();

        try {
            HarvestableRefConverter href1 = new HarvestableRefConverter();
            href1.setId(new Long(1));
            href1.setLastUpdated(new Date());
            href1.setResourceUri(new URI("http://localhost/harvestables/1/"));

            HarvestableRefConverter href2 = new HarvestableRefConverter();
            href2.setId(new Long(2));
            href2.setLastUpdated(new SimpleDateFormat("MM/dd/yy").parse("05/05/2008"));
            href2.setResourceUri(new URI("http://localhost/harvestable/2/"));
            hrefs.add(href1);
            hrefs.add(href2);
        } catch (Exception e) {
            logger.log(Level.INFO, "Cannot create test entities", e);
        }

        return hrefs;
    }

    public Harvestable retrieveFromRef(HarvestableRefConverter href) {
        OaiPmhResource hable = new OaiPmhResource();
        hable.setId(href.getId());
        hable.setLastUpdated(href.getLastUpdated());
        hable.setName("some generated name");
        hable.setTitle("some generated title");
        hable.setScheduleString("* * * * *");
        hable.setUrl("http://heinonline.org/HOL/OAI");
        hable.setMetadataPrefix("oai_dc");
        
        return hable;
    }

    public void updateHarvestable(Harvestable harvestable) {
        logger.log(Level.INFO, "harvestable updated");
    }
}
