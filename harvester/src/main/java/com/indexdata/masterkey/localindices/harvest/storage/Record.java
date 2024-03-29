package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public interface Record {

  String getId();

  String getDatabase();

  boolean isDeleted();

  /**
   * Original content of the record, if preserved, otherwise null
   * @return raw original content
   */
  byte[] getOriginalContent();

  void setOriginalContent(byte[] content);

  Map<String, Collection<Serializable>> getValues();

  boolean isCollection();

  Collection<Record> getSubRecords();

  void setCreationTiming(long timing);
  long getCreationTiming();
  void setTransformationTiming(long timing);
  long getTransformationTiming();

}
