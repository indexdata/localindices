/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage.folio;

import java.net.URI;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

/**
 *
 * @author ne
 */
public class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {

  public static final String METHOD_NAME = "DELETE";

  @Override
  public String getMethod() {
      return METHOD_NAME;
  }

  public HttpDeleteWithBody(final String uri) {
      super();
      setURI(URI.create(uri));
  }

}
