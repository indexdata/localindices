package com.indexdata.masterkey.harvest.oai;

/**
 * Harvesting status codes.
 * @author jakub
 */
public enum HarvestStatus {
    NEW,
    WAITING,
    RUNNING,
    FINISHED,
    KILLED,
    ERROR
}
