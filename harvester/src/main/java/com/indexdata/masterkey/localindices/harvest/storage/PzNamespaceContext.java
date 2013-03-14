package com.indexdata.masterkey.localindices.harvest.storage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import javax.xml.namespace.NamespaceContext;

public class PzNamespaceContext implements NamespaceContext {
  static HashMap<String,String> map = new LinkedHashMap<String,String>();
  static {
    map.put("pz", "http://www.indexdata.com/pazpar2/1.0");
  }
  @Override
  public String getNamespaceURI(String prefix) {
    return map.get(prefix);
  }

  @Override
  public String getPrefix(String namespaceURI) {
    for (String prefix : map.keySet()) {
      if (map.get(prefix).equals(namespaceURI))
      	return prefix;
    }
    return null;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Iterator getPrefixes(String namespaceURI) {
    Collection<String> list = new LinkedList<String>();
    for (String prefix : map.keySet()) {
      if (map.get(prefix).equals(namespaceURI))
      	list.add(prefix);
    }
    return list.iterator();
  }

}
