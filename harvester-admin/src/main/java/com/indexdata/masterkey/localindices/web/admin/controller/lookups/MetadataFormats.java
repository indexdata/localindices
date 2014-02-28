package com.indexdata.masterkey.localindices.web.admin.controller.lookups;

import java.util.ArrayList;
import java.util.List;

public class MetadataFormats extends OaiPmhResponse {

  private static final long serialVersionUID = -8291777485549251263L;
  
  List<MetadataFormat> metadataformats = new ArrayList<MetadataFormat>();
  
  public List<MetadataFormat> getMetadataFormats () {
    if (metadataformats.size()==0) {
      if (getElements("metadataFormat") != null) {
        for (ResponseDataObject element : getElements("metadataFormat")) {
          metadataformats.add((MetadataFormat)element);
        }
      }
    }
    return metadataformats;
  }

  public int count() {
    return metadataformats.size();
  }
}
