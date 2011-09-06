package com.indexdata.masterkey.localindices.entity;

import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement(name = "basicTransformStep")
public class BasicTransformationStep extends TransformationStep {

  public BasicTransformationStep() {
  }

  public BasicTransformationStep(String name, String description, String script) {
    this.name = name;
    this.description = description;
    this.script = script;
  }

  private static final long serialVersionUID = 4552043105886552958L;

}
