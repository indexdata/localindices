package com.indexdata.masterkey.harvest.oai;


/**
   Copyright 2008 Index Data ApS
   http://www.indexdata.com
   Licensed under the GNU Public License, Version 2.0.
*/

public interface HarvestJob extends Runnable {
    
    public void kill();
    public HarvestStatus getStatus();
    public void setStorage(HarvestStorage storage);
    public void finishReceived();
    public String getError();
    
}
