package com.indexdata.masterkey.localindices.entity;

import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement(name = "splitStep")
public class SplitStep extends TransformationStep {

  public SplitStep()  
  {
  }

  private static final long serialVersionUID = 4552043105886552958L;

  public String getSplitAt() {
    int pos = script.indexOf(":");
    if (pos > 0)
      return script.substring(0, pos-1);
    return "";
  }

  public void setSplitAt(String splitAt) {

  }

  public String getSplitSize() {
    int pos = script.indexOf(":");
    if (pos >= 0)
      return script.substring(pos+1);
    return "";
  }

  public void setSplitSize(String splitSize) {

  }

}
