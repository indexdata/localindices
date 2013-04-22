package ORG.oclc.oai.harvester2.verb;

import org.w3c.dom.Document;

public class OaiPmhException extends RuntimeException {
  
  /**
   * 
   */
  private static final long serialVersionUID = -7526812395539954035L;
	String message;
  	Document document;
  	
  	public OaiPmhException(String message, Document doc) {
  	  this.message = message; 
  	  document = doc; 
  	}

  	public String getMessage() {
  	  return message;
  	}
  	
  	public Document getDocument() {
  	  return document;
  	}

}
