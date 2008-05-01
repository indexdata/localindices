package com.indexdata.masterkey.harvest.oai;


/**
   Copyright 2008 Index Data ApS
   http://www.indexdata.com
   Licensed under the GNU Public License, Version 2.0.
*/

import java.io.OutputStream;

public interface HarvestStorage {
    public OutputStream getOutputStream();    
}
