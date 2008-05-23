/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

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
