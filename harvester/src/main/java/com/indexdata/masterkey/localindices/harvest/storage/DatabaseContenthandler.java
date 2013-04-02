package com.indexdata.masterkey.localindices.harvest.storage;

import org.xml.sax.ContentHandler;

public interface DatabaseContenthandler extends ContentHandler {

  void   setDatebaseIdentifier(String id);
  String getDatebaseIdentifier();
  
}
