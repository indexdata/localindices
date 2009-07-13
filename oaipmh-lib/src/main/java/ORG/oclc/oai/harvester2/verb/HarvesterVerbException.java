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
    String requestURL;

    public HarvesterVerbException(String msg, Throwable cause, InputStream responseStream, String requestURL) {
        super(msg, cause);
        this.requestURL = requestURL;
        this.responseStream = responseStream;
    }
    
    public InputStream getResponseStream() {
        return responseStream;
    }

    public String getRequestURL() {
        return requestURL;
    }
    
}
