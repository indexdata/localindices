package com.indexdata.masterkey.localindices.web.admin.help;

import java.util.StringTokenizer;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean(name = "contextHelp")
@ViewScoped
public class ContextHelp {

  String area = "";
  String sectionId = "";
  String sectionText = "";
  String labelId = "";
  String labelText = "";
  
  public ContextHelp() {
  }

  public void setField (String field) {
    StringTokenizer tokens = new StringTokenizer(field,"[]");
    area = tokens.nextToken();
    setSection(tokens.nextToken());
    setLabel(tokens.nextToken());
  }
  
  public String getField () {
    return area + "." + sectionId + "." + labelId;
  }
  
  public void setArea (String area) {
    this.area = area;
  }
  
  public String getArea () {
    return area;
  }
  
  public void setSection (String section) {
    this.sectionText = section;
    this.sectionId = section.replaceAll(" ", "_").replaceAll(":","");
  }
  public String getSection () {
    return getSectionText();
  }
  
  public String getSectionText() {
    return sectionText;
  }
  
  public String getSectionId() {
    return sectionId;
  }
  
  public void setLabel (String label) {
    labelText = label;
    labelId = label.replaceAll(" ","_").replaceAll(":","");
  }
  
  public String getLabel () {
    return getLabelText();
  }
  
  public String getLabelId () {
    return this.labelId;
  }
  
  public String getLabelText() {
    return this.labelText;
  }
  
}
