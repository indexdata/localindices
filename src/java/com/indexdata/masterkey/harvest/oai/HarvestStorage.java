package com.indexdata.masterkey.harvest.oai;


/**
   Copyright 2008 Index Data ApS
   http://www.indexdata.com
   Licensed under the GNU Public License, Version 2.0.
*/

/* package ; */

//import java.util.Date;
import java.io.IOException;

public interface HarvestStorage {

    public void store(HarvestJob job, String content) throws IOException;

    //public abstract String baseURL();
    //public abstract String job();
    //public abstract String part();
    //public abstract String batch();
    //public abstract String status();
    //public abstract String type();

    
}
