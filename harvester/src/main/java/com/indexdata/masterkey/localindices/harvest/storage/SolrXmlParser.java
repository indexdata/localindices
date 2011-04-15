package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.InputStream;
import java.io.StringReader;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

public class SolrXmlParser {

	XMLInputFactory factory;
	Stack<XmlHandler> handlers = new Stack<XmlHandler>();
	public SolrXmlParser() {
		factory = XMLInputFactory.newInstance();
	}


	public void parse(String xml, XmlHandler context) throws XMLStreamException 
	{
		StringReader reader = new StringReader(xml);
		XMLStreamReader r = factory.createXMLStreamReader(reader);
	    handleEvent(r, context);
	}

	public void parse(InputStream input, XmlHandler context) throws XMLStreamException 
	{
		XMLStreamReader r = factory.createXMLStreamReader(input);
	    handleEvent(r, context);
	}


	protected void handleEvent(XMLStreamReader parser, XmlHandler context) throws XMLStreamException 
	{
		while (parser.hasNext()) {
			int event = parser.next();
			switch (event) {
	        case XMLEvent.START_DOCUMENT:
				context = context.handleStartDocument(parser);
				//handlers.push(newContext);
				break;
	        case XMLEvent.END_DOCUMENT:
	        	//context = handlers.pop();
				context = context.handleEndDocument(parser);
				break;
	        case XMLEvent.START_ELEMENT:
				handlers.push(context);
				context = context.handleStartElement(parser);
				break;
	        case XMLEvent.END_ELEMENT:
	        	context = handlers.pop();
				context.handleEndElement(parser);
				break;
	        case XMLEvent.PROCESSING_INSTRUCTION:
				context = context.handleProcessingInstruction(parser);
				break;
	        case XMLEvent.CHARACTERS:
				context = context.handleCharacters(parser);
				break;
	        case XMLEvent.COMMENT:
				context = context.handleComment(parser);
				break;
	        case XMLEvent.ENTITY_REFERENCE:
				context = context.handleEntityReference(parser);
				break;
	        case XMLEvent.ATTRIBUTE:
				context = context.handleAttribute(parser);
				break;
	        case XMLEvent.DTD:
				context = context.handleDTD(parser);
				break;
	        case XMLEvent.CDATA:
				context = context.handleCDATA(parser);
				break;
	        case XMLEvent.SPACE:
				context = context.handleSPACE(parser);
				break;
	        default:
	        	System.out.println("Unhandled event: " + event);
	        	break;
	        	
			}
		}
	}
}
