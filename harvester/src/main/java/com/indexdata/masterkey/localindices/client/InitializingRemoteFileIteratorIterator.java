/*
 * Copyright (c) 1995-2014, Index Data
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
public class InitializingRemoteFileIteratorIterator implements RemoteFileIterator {
  
  private final LinkedList<NotInitializedRemoteFileIterator> iterators =  new LinkedList<NotInitializedRemoteFileIterator>();
  
  public boolean add(NotInitializedRemoteFileIterator iterator) {
    return iterators.add(iterator);
  }
  
  @Override
  public boolean hasNext() throws IOException {
    NotInitializedRemoteFileIterator head;
    while ((head = iterators.peek()) != null) {
      if (!head.isInitialized()) head.init(); //this executes the connection
      if (head.hasNext()) return true;
      iterators.pop();
    }
    return false;
  }
  
  @Override
  public RemoteFile getNext() throws IOException {
    NotInitializedRemoteFileIterator head;
    while ((head = iterators.peek()) != null) { //if still has iterator
      if (!head.isInitialized()) head.init(); //this executes the connection
      if (head.hasNext()) { //and iterator has values
        return head.getNext();
      }
      iterators.pop();
    }
    return null;
  }
  
}
