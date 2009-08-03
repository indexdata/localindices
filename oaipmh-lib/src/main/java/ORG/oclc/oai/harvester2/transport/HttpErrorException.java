/*
 * Copyright (c) 1995-2009, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package ORG.oclc.oai.harvester2.transport;

import java.io.IOException;

/**
 *
 * @author jakub
 */
public class HttpErrorException extends IOException {
    private int statusCode;
    private String statusMessage;
    private String url;

    public HttpErrorException(int code, String message, String url) {
        super("Http error '" + code + " " + message + "' when contacting " + url);
        statusCode = code;
        statusMessage = message;
        this.url = url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getUrl() {
        return url;
    }
    
}
