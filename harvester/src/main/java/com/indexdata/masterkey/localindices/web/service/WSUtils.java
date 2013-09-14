/*
 * Copyright (c) 1995-2013, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.web.service;

import javax.ws.rs.core.Response;

/**
 *
 * @author jakub
 */
public class WSUtils {
  
  public static Response buildError(int code, String msg) {
    return Response.status(code).entity(msg).type(
      "text/html").build();
  }
  
}
