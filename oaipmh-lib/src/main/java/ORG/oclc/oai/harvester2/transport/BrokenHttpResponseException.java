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
public class BrokenHttpResponseException extends IOException {

    public BrokenHttpResponseException(String message) {
        super(message);
    }

}
