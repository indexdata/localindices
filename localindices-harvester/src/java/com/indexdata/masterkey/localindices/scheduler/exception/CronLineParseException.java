/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.scheduler.exception;

/**
 * Thrown if a specified string is a malformed Cron line.
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
