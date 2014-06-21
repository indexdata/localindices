package com.indexdata.masterkey.localindices.web.admin.help;

import java.util.StringTokenizer;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean(name = "contextHelp")
@ViewScoped
public class ContextHelp {

  String area = "";
  String section = "";
  String label = "";
  
  public ContextHelp() {
  }

  public void setField (String field) {
    if (field.contains(".")) {
      StringTokenizer tokens = new StringTokenizer(field,".");
      area = tokens.nextToken();
      section = tokens.nextToken();
      label = tokens.nextToken();
    }
  }
  
  public String getField () {
    return area + "." + section + "." + label;
  }
  
  public void setArea (String area) {
    this.area = area;
  }
  
  public String getArea () {
    return area;
  }
  
  public void setSection (String section) {
    this.section = section.replaceAll(" ", "_");
  }
  public String getSection () {
    return section.replaceAll("_", " ");
  }
  
  public void setLabel (String label) {
    this.label = label.replaceAll(" ", "_");
  }
  
  public String getLabel () {
    return this.label.replaceAll("_", " ");
  }
  
}
