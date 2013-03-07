package ORG.oclc.oai.harvester2.verb;

import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import ORG.oclc.oai.harvester2.verb.HarvesterVerb;

public class OaiPmhNamespaceContext implements NamespaceContext {

  public String getNamespaceURI(String prefix) {
      if (prefix == null) throw new NullPointerException("Null prefix");
      else if ("oai20".equals(prefix)) 
	return HarvesterVerb.NAMESPACE_V2_0;
      else if ("oai11_list_records".equals(prefix)) 
	return HarvesterVerb.NAMESPACE_V1_1_LIST_RECORDS;
      else if ("xml".equals(prefix)) 
	return XMLConstants.XML_NS_URI;
      return XMLConstants.NULL_NS_URI;
  }

  // This method isn't necessary for XPath processing.
  public String getPrefix(String uri) {
      throw new UnsupportedOperationException();
  }

  // This method isn't necessary for XPath processing either.
  public Iterator<Object> getPrefixes(String uri) {
      throw new UnsupportedOperationException();
  }

}
