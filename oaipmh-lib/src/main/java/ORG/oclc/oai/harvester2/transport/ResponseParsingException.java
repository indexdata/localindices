/*
 *  Copyright (c) 1995-2008, Index Data
 *  All rights reserved.
 *  See the file LICENSE for details.
 */

package ORG.oclc.oai.harvester2.transport;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author jakub
 */
public class ResponseParsingException extends IOException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -4509192622653512403L;
	InputStream responseStream;
    String requestURL;

    public ResponseParsingException(String msg, Throwable cause, InputStream responseStream, String requestURL) {
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
