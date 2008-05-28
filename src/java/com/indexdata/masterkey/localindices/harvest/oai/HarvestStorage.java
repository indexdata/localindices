/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.oai;


/**
   Copyright 2008 Index Data ApS
   http://www.indexdata.com
   Licensed under the GNU Public License, Version 2.0.
*/

import java.io.IOException;
import java.io.OutputStream;

public interface HarvestStorage {
    public void openOutput() throws IOException;
    public void closeOutput() throws IOException;
    public OutputStream getOutputStream();    
    public void removeAll() throws IOException;
}
