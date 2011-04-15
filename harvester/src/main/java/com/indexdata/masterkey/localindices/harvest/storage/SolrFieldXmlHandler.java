package com.indexdata.masterkey.localindices.harvest.storage;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;

import org.apache.solr.common.SolrInputDocument;

public class SolrFieldXmlHandler implements XmlHandler {

	XmlHandler parent; 
	SolrInputDocument doc;
	String name;
	StringBuffer characters = new StringBuffer();
	Map<String, String> attributes;

	public SolrFieldXmlHandler(XmlHandler parent, SolrInputDocument doc) {
		this.doc = doc;
		this.parent = parent;
	}
	@Override

	public XmlHandler handleStartDocument(XMLStreamReader parser) {
		return this;
	}

	@Override
	public XmlHandler handleEndDocument(XMLStreamReader parser) {
		return this;
	}

	
	Map<String,String> getAttributes(XMLStreamReader parser) {
		Map<String, String> map = new HashMap<String,String>();
		for (int index = 0; index < parser.getAttributeCount(); index++) {
			String name  = parser.getAttributeLocalName(index);
			String value = parser.getAttributeValue(index);
			map.put(name, value);
			
		}
		return map;
	}
	
	@Override
	public XmlHandler handleStartElement(XMLStreamReader parser) {
		if (parser.getLocalName().equals("field")) {
			characters = new StringBuffer();
			name = getAttributes(parser).get("name");
		}
		return this;
	}

	@Override
	public XmlHandler handleEndElement(XMLStreamReader parser) {
		if (parser.getLocalName().equals("field")) {
			if (doc != null && name != null) {
				System.out.println("field: " + name + " " + (characters != null? characters.toString(): "null") );
				doc.addField(name, characters);
				characters = null;
				name = null;
			}
		}
		return this;
	}

	@Override
	public XmlHandler handleProcessingInstruction(XMLStreamReader parser) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public XmlHandler handleCharacters(XMLStreamReader parser) {
		handleText(parser.getText());
		return this;
	}

	@Override
	public XmlHandler handleComment(XMLStreamReader parser) {
		return this;
	}

	@Override
	public XmlHandler handleEntityReference(XMLStreamReader parser) {
		return this;
	}

	private void handleText(String text) {
		if (characters != null) {
			characters.append(text);
		}
	}
	
	@Override
	public XmlHandler handleAttribute(XMLStreamReader parser) 
	{
		// TODO fix, never used. 
		if (parser.getLocalName().equals("name")) 
			name = parser.getAttributeValue(0); 
		return this;
	}

	@Override
	public XmlHandler handleDTD(XMLStreamReader parser) {
		return this;
	}

	@Override
	public XmlHandler handleCDATA(XMLStreamReader parser) {
		handleText(parser.getText());
		return this;
	}

	@Override
	public XmlHandler handleSPACE(XMLStreamReader parser) {
		handleText(parser.getText());
		return this;
	}

}
