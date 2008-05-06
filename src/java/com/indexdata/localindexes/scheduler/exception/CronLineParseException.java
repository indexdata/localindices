/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
