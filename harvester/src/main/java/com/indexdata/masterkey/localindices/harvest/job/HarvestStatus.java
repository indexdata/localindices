/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.harvest.job;

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
