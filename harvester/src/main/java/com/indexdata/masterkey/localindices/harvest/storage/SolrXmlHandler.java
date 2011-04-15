package com.indexdata.masterkey.localindices.harvest.storage;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.solr.common.SolrInputDocument;

public class SolrXmlHandler implements XmlHandler {
	
	private SolrFieldXmlHandler childHandler = null;  
	private List<SolrInputDocument> documents = new LinkedList<SolrInputDocument>();
	
	Collection<SolrInputDocument> getDocuments() { return documents; }; 
	
	@Override
	public XmlHandler handleStartDocument(XMLStreamReader parser) {
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

	@Override
	public XmlHandler handleStartElement(XMLStreamReader parser) {
		if (parser.getLocalName().equals("doc")) {
			SolrInputDocument doc = new SolrInputDocument();
			documents.add(doc);
			childHandler = new SolrFieldXmlHandler(this, doc);
			return childHandler;
		}
		return this;
	}

	@Override
	public XmlHandler handleEndElement(XMLStreamReader parser) {
		return this;
	}

	@Override
	public XmlHandler handleProcessingInstruction(XMLStreamReader parser) {
		return this;
	}

	@Override
	public XmlHandler handleCharacters(XMLStreamReader parser) {
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
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public XmlHandler handleSPACE(XMLStreamReader parser) {
		// TODO Auto-generated method stub
		return this;
	}

}
