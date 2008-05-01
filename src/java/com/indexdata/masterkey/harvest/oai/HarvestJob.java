package com.indexdata.masterkey.harvest.oai;


/**
   Copyright 2008 Index Data ApS
   http://www.indexdata.com
   Licensed under the GNU Public License, Version 2.0.
*/

public interface HarvestJob extends Runnable {

    public static final int STATUS_NEW = 1; 
    public static final int STATUS_ACTIVE = 2; 
    public static final int STATUS_FINISHED = 3; 
    public static final int STATUS_KILL = 4; 
    public static final int STATUS_ERROR = 5; 

    public void setStorage(HarvestStorage storage);

}
