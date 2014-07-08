/*
 * Copyright (c) 1995-2014, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.client;

import java.io.IOException;
import java.util.LinkedList;

/**
 * Iterates over
 * @author jakub
 */
public class RemoteFileIteratorIterator implements RemoteFileIterator {
  
  private final LinkedList<RemoteFileIterator> iterators =  new LinkedList<RemoteFileIterator>();
  
  public boolean add(RemoteFileIterator iterator) {
    return iterators.add(iterator);
  }
  
  @Override
  public boolean hasNext() throws IOException {
    RemoteFileIterator head;
    while ((head = iterators.peek()) != null) {
      if (head.hasNext()) return true;
      iterators.pop();
    }
    return false;
  }
  
  @Override
  public RemoteFile getNext() throws IOException {
    RemoteFileIterator head;
    while ((head = iterators.peek()) != null) {
      if (head.hasNext()) return head.getNext();
      iterators.pop();
    }
    return null;
  }
  
}
