package com.indexdata.masterkey.harvest.oai;


/**
   Copyright 2008 Index Data ApS
   http://www.indexdata.com
   Licensed under the GNU Public License, Version 2.0.
*/

/* package ; */

import java.util.Date;


public abstract class HarvestJob implements java.lang.Runnable {

    // public static final String JOB_TYPE; 

    public static final String STATUS_NEW = "new"; 
    public static final String STATUS_ACTIVE = "active"; 
    public static final String STATUS_FINISHED = "finished"; 
    public static final String STATUS_KILL = "kill"; 
    public static final String STATUS_ERROR = "error"; 

    //public HarvestJob (String baseURL,
    //                      String from,
    //                      String until,
    //                      String metadataPrefix,
    //                   String setSpec);

    public abstract void run(); 

    public abstract String baseURL();
    public abstract String job();
    public abstract String part();
    public abstract String batch();
    public abstract String status();
    public abstract String type();

    public String message() {
        Date now = new Date();
        String message = now + " " + type() + " " + baseURL() + " " + job() 
            + " " + part() + " " + batch() + " " + status();
        return message;
    }

    public void setStorage(HarvestStorage storage) {
        this.storage = storage;
    };
    
    protected HarvestStorage storage = null;

}
