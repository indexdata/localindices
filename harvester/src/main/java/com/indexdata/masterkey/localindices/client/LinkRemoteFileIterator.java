package com.indexdata.masterkey.localindices.client;

import java.util.ArrayList;

public class LinkRemoteFileIterator implements RemoteFileIterator {

  ArrayList<RemoteFile> files; ;
  int index = 0;

  LinkRemoteFileIterator() {
    files = new ArrayList<RemoteFile>();  
  }

  LinkRemoteFileIterator(ArrayList<RemoteFile> httpLinks) {
    files = httpLinks;
  }
  
  @Override
  public boolean hasNext() {
    return index < files.size(); 
  }

  @Override
  public RemoteFile get() {
    return files.get(index++);
  }

  public void add(RemoteFile file) {
    files.add(file);
  }
}
