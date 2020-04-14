package com.indexdata.masterkey.localindices.web.admin.help;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.StringTokenizer;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean(name = "contextHelp")
@ViewScoped
public class ContextHelp {

  String areaId = "";
  String areaText = "";
  String sectionId = "";
  String sectionText = "";
  String labelId = "";
  String labelText = "";
  
  public ContextHelp() {
  }

  public void setField (String field) {
    StringTokenizer tokens = new StringTokenizer(field,"[]");
    setArea(tokens.nextToken());
    setSection(tokens.nextToken());
    setLabel(tokens.nextToken());
  }
  
  public String getField () {
    return areaId + "." + sectionId + "." + labelId;
  }
  
  public void setArea (String area) {
    this.areaText = area;
    this.areaId = area.replaceAll(" ", "_").replaceAll("[:=]","");;
  }
  
  public String getArea () {
    return getAreaText();
  }
  
  public String getAreaText() {
    return areaText;
  }
  
  public String getAreaId() {
    return areaId;
  }
  
  public void setSection (String section) {
    this.sectionText = section;
    this.sectionId = section.replaceAll(" ", "_").replaceAll("[:=]","");
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
    labelId = label.replaceAll(" ","_").replaceAll("[:=]","");
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
  
  public String urlEncode (String parameter) {
    try {
      return URLEncoder.encode(parameter, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return parameter;
    }
  }
}
