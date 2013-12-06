/*
 * Copyright (c) 1995-2013, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package ORG.oclc.oai.harvester2.data;

import java.io.InputStream;

/**
 *
 * @author jakub
 */
public class IdentityInputStreamWrapper implements InputStreamWrapper {

  @Override
  public InputStream wrap(InputStream is) {
    return is;
  }
  
}
