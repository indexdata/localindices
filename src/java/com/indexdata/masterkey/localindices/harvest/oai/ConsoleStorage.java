/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.oai;

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

    public void begin() throws IOException {
        throw new UnsupportedOperationException("This stream does not need to opened.");
    }

    public void commit() throws IOException {
        throw new UnsupportedOperationException("This stream does not need to be closed.");
    }
    public void purge() throws IOException {
        System.out.println("Storage.Removeall: Please discard the previous output");
    }

    public void rollback() throws IOException {
        System.out.println("Storage.Removeall: Please discard the previous output");
    }
}
