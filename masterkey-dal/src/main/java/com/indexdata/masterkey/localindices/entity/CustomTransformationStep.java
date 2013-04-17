package com.indexdata.masterkey.localindices.entity;

import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement(name = "CustomTransformStep")
public class CustomTransformationStep extends TransformationStep {

  public CustomTransformationStep() {
  }

  public CustomTransformationStep(String name, String description, String script, String customClass) {
    this.name = name;
    this.description = description;
    this.script = script;
    this.customClass = customClass;
  }

  private static final long serialVersionUID = 4552043105886552958L;

}
