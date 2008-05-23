/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.localindexes.scheduler.exception;

/**
 *
 * @author jakub
 */
public class CronLineParseException extends IllegalArgumentException {
    public CronLineParseException (String msg, Exception e) {
        super(msg,e);
    }
    
    public CronLineParseException(String msg) {
        super(msg);
    }
}
