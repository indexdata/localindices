/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.web.service.client;

/**
 * General resource connection exception
 * @author jakub
 */
public class ResourceConnectionException extends Exception {

    public ResourceConnectionException(String message) {
        super(message);
    }
    
    public ResourceConnectionException(Exception cause) {
        super(cause);
    }
    
}
