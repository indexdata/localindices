package com.indexdata.masterkey.localindices.harvest.storage;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.solr.common.SolrInputDocument;

public class PrintXmlHandler implements XmlHandler {
	
	// private SolrFieldXmlHandler childHandler = null;  
	private List<SolrInputDocument> documents = new LinkedList<SolrInputDocument>();
	private StringBuffer indent = new StringBuffer(""); 
	Collection<SolrInputDocument> getDocuments() { return documents; }; 
	private StringBuffer text = new StringBuffer(""); 
	
	void indent() {
		indent.append("   ");
	}

	void deindent() {
		if (indent.length() - 3 >= 0)
			indent.setLength(indent.length() - 3);
		else 
			indent.setLength(0);
	}

	@Override
	public XmlHandler handleStartDocument(XMLStreamReader parser) {
		if (parser.getVersion() != null)
			System.out.print(parser.getVersion());
		return this;
	}

	@Override
	public XmlHandler handleEndDocument(XMLStreamReader parser) {
		try {
			parser.close();
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this;
	}

	String printAttributes(XMLStreamReader parser) {
		// TODO
		return ""; 
	}
	
	@Override
	public XmlHandler handleStartElement(XMLStreamReader parser) {
		System.out.println(text);
		text.setLength(0);
		System.out.print(indent + "<" + parser.getLocalName() + printAttributes(parser) + ">");
		indent();
		return this;
	}

	@Override
	public XmlHandler handleEndElement(XMLStreamReader parser) {
		deindent();
		System.out.print(text);
		text.setLength(0);
		System.out.println("</" + parser.getLocalName() + ">");
		return this;
	}

	@Override
	public XmlHandler handleProcessingInstruction(XMLStreamReader parser) {
		return this;
	}

	@Override
	public XmlHandler handleCharacters(XMLStreamReader parser) {
		text.append(parser.getText());
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

	@Override
	public XmlHandler handleAttribute(XMLStreamReader parser) {
		return this;
	}

	@Override
	public XmlHandler handleDTD(XMLStreamReader parser) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public XmlHandler handleCDATA(XMLStreamReader parser) {
		text.append(parser.getText());
		return this;
	}

	@Override
	public XmlHandler handleSPACE(XMLStreamReader parser) {
		text.append(parser.getText());
		return this;
	}

}
