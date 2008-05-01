package com.indexdata.masterkey.harvest.oai;

import java.io.OutputStream;

/**
 * Simple console storage for harvested records
 * @author jakub
 */
public class ConsoleStorage implements HarvestStorage {

    public OutputStream getOutputStream() {
        return System.out;
    }
    
}
