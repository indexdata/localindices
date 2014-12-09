package com.indexdata.masterkey.localindices.client.filefilters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompositeEntryFilter implements EntryFilter {

  List<EntryFilter> filters = new ArrayList<EntryFilter>();

  public CompositeEntryFilter(EntryFilter...entryFilters) {
    filters = Arrays.asList(entryFilters);
  }
  
  public void addFilters(EntryFilter...entryFilters) {
    filters.addAll(Arrays.asList(entryFilters));
  }

  @Override
  public boolean accept(EntryFilteringInfo info) {
    for (EntryFilter filter : filters) {
      if (! filter.accept(info)) {
        return false;
      }
    }
    return true;
  }

}
