/*
 * Copyright (c) 1995-2013, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package ORG.oclc.oai.harvester2.data;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author jakub
 */
public interface InputStreamWrapper {
  InputStream wrap(InputStream is) throws IOException;
}
