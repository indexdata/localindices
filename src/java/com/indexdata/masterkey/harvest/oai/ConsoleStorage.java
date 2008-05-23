/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.harvest.oai;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Simple console storage for harvested records
 * @author jakub
 */
public class ConsoleStorage implements HarvestStorage {

    public OutputStream getOutputStream() {
        return System.out;
    }

    public void openOutput() throws IOException {
        throw new UnsupportedOperationException("This stream does not need to opened.");
    }

    public void closeOutput() throws IOException {
        throw new UnsupportedOperationException("This stream does not need to be closed.");
    }
    
}
