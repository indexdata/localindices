package com.indexdata.masterkey.localindices.entity;

import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement(name = "basicTransformation")
public class BasicTransformation extends Transformation {

  public BasicTransformation() {
  };

  private static final long serialVersionUID = -1789712151621170759L;

}
