package com.indexdata.masterkey.localindices.entity;

import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement(name = "XmlTransformStep")
public class XmlTransformationStep extends TransformationStep {

  public XmlTransformationStep() {
  }

  public XmlTransformationStep(String name, String description, String script) {
    this.name = name;
    this.description = description;
    this.script = script;
  }

  private static final long serialVersionUID = 4552043105886552958L;

}
