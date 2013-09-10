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
  String responseStream;
  String requestURL;

  public ResponseParsingException(String msg, Throwable cause,
    String responseString, String requestURL) {
    super(msg, cause);
    this.requestURL = requestURL;
    this.responseStream = responseStream;
  }

  public String getResponseString() {
    return responseStream;
  }

  public String getRequestURL() {
    return requestURL;
  }
}
