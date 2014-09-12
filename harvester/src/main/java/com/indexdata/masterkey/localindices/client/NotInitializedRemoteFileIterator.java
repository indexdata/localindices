package com.indexdata.masterkey.localindices.client;


import com.indexdata.masterkey.localindices.client.RemoteFileIterator;
import java.io.IOException;

/*
 * Copyright (c) 1995-2014, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */

/**
 *
 * @author jakub
 */
public interface NotInitializedRemoteFileIterator extends RemoteFileIterator {
  void init() throws IOException;
  boolean isInitialized();
}
