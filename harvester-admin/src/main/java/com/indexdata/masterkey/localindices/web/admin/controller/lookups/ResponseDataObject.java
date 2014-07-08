package com.indexdata.masterkey.localindices.web.admin.controller.lookups;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Represents an XML response as recursive lists of ResponseDataObjects.
 *  
 * @author Niels Erik
 *
 */
public class ResponseDataObject implements Serializable {

  Logger logger = Logger.getLogger(ResponseDataObject.class);
  private static final long serialVersionUID = -3909755656714679959L;
  String type = null;
  HashMap<String,String> attributes = new HashMap<String,String>();
  HashMap<String,List<ResponseDataObject>> elements = new HashMap<String,List<ResponseDataObject>>();
  String textContent = "";
  String xml = null;
  boolean isBinary = false;
  byte[] binary = null;
        
  public void setType (String type) {
    this.type = type;
  }
  
  public String getType () {
    return type;
  }
  
  public void setAttribute (String name, String value) {
    attributes.put(name, value);
  }
  
  public String getAttribute (String name) {
    return attributes.get(name);
  }
    
  /**
   * Used by a client (the parser) to add child element objects
   * 
   * @param name of the child element
   * @param value the child object itself
   */
  public void addElement (String name, ResponseDataObject value) {
    if (elements.containsKey(name)) {
      elements.get(name).add(value);
    } else {
      List<ResponseDataObject> list = new ArrayList<ResponseDataObject>();
      list.add(value);      
      elements.put(name,list);
    }
  }
  
  /**
   * Retrieves list of child elements as ResponseDataObjects
   * 
   * @param name
   * @return
   */
  public List<ResponseDataObject> getElements (String name) {
    return elements.get(name);
  }
    
  public ResponseDataObject getOneElement (String name) {
    if (elements.get(name) != null) {
      return elements.get(name).get(0);
    } else {
      return null;
    }
  }
  
  /**
   * Returns the text content of the first element found with the given
   * name
   * @param name of the element 
   * @return text value, empty string if none found
   */
  public String getOneValue (String name) {
    if (getOneElement(name)!=null && getOneElement(name).getValue().length()>0) {
      return getOneElement(name).getValue();
    } else {
      return "";
    }
  }
  
  /**
   * Returns string array with the values of the named element(s)
   *   
   * @param name of the child object(s) to retrieve value(s) from
   * @return
   */
  public String[] getValueArray (String name) {
    List<ResponseDataObject> elements = getElements(name);
    String[] valueArray = {};
    if (elements != null) {
      valueArray = new String[elements.size()];
      int i = 0;
      for (ResponseDataObject element : elements) {
        valueArray[i++] = element.getValue();
      }      
    }
    return valueArray;
  }
    
  public void appendContent (String content) {
    textContent = textContent + content;
  }
  
  public String getValue () {
    return textContent;
  }
  
  public String getProperty(String name) {
    List<ResponseDataObject> els = elements.get(name);
    if (els != null) {
      return els.get(0).getValue();
    } else {     
      return null;
    }
  }
  
  public int getIntValue(String name) {
    String val = getOneValue(name);
    if (val.length()==0) {
      return 0;
    } else {
      return Integer.parseInt(val);
    }
  }
    
  public boolean hasApplicationError () {
    return (getOneElement("applicationerror") != null);   
  }
  
  public void setXml(String xml) {
    this.xml = xml; 
  }
  
  public String getXml() {
    if (type != null && type.equals("record")) {
      logger.debug("Getting XML for "+type + ": "+xml);
    }      
    return xml == null ? "" : xml;
  }
  
  public boolean getHasResults () {
    return (xml != null && xml.length()>0) || (getIsBinary() && binary.length>0);
  }
  
  public boolean getIsBinary () {
    return isBinary;
  }
    
  public void setBinary(byte[] bytes) {
    isBinary = true;
    binary = bytes;
  }
  
  public byte[] getBinary () {
    return binary;
  }
  
  
        
}
