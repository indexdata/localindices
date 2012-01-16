package com.indexdata.masterkey.localindices.harvest.storage;

import java.util.LinkedList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

public class CleanXMLContentHandler extends DefaultHandler2 {
  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts)
      throws SAXException {
    
    super.startElement(uri, localName, qName, atts);
    if (elements.contains(localName) || elements.contains(qName)) {
      super.startCDATA();      
    }
    
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if (elements.contains(localName) || elements.contains(qName)) {
      super.endCDATA();      
    }
    super.endElement(uri, localName, qName);
  }
  
  private List<String> elements = new LinkedList<String>();
  
  void addProtectedElement(String element) {
    	elements.add(element);
  }
}
