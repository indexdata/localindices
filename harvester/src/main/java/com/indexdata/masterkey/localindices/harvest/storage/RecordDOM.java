package com.indexdata.masterkey.localindices.harvest.storage;

import org.w3c.dom.Node;

public interface RecordDOM extends Record {

  Node toNode();
  void setNode(Node newNode);
}
