/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.harvest.storage;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.entity.WebCrawlResource;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;

/**
 * Returns an instance of a HarvestStorage object.
 * @author jakub
 */
public class HarvestStorageFactory {

    public HarvestStorageFactory() {
    }

    public static HarvestStorage getStorage(String storageDir, Harvestable harvestable) {
        HarvestStorage st = null;
        if (harvestable instanceof OaiPmhResource) {
            if (((OaiPmhResource) harvestable).getMetadataPrefix().equalsIgnoreCase("marc21")) {
                st = new ZebraFileStorage(storageDir, harvestable, "oaimarc21-pz.xml");
            } else {
                st = new ZebraFileStorage(storageDir, harvestable, "oaidc-pz.xml");
            }
        } else if (harvestable instanceof XmlBulkResource) {
            st = new ZebraFileStorage(storageDir, harvestable, "marc-pz.xml");
        } else if (harvestable instanceof WebCrawlResource) {
            st = new ZebraFileStorage(storageDir, harvestable, "pz-pz.xml");
            st.setOverwriteMode(true);
        }
        return st;
    }
}
