/*
 *  Copyright (c) 1995-2008, Index Data
 *  All rights reserved.
 *  See the file LICENSE for details.
 */

package ORG.oclc.oai.harvester2.verb;

import java.io.InputStream;

/**
 *
 * @author jakub
 */
public class HarvesterVerbException extends Exception {
    InputStream responseStream;

    public HarvesterVerbException(String msg, Throwable cause, InputStream responseStream) {
        super(msg, cause);
        this.responseStream = responseStream;
    }
    
    public InputStream getResponseStream() {
        return responseStream;
    }
}
