package com.indexdata.masterkey.localindices.harvest.storage;

import javax.xml.stream.XMLStreamReader;

public interface XmlHandler {

	XmlHandler handleStartDocument(XMLStreamReader parser);
	XmlHandler handleEndDocument(XMLStreamReader parser);

	XmlHandler handleStartElement(XMLStreamReader parser);
	XmlHandler handleEndElement(XMLStreamReader parser);
	
	XmlHandler handleProcessingInstruction(XMLStreamReader parser);
	XmlHandler handleCharacters(XMLStreamReader parser);
	XmlHandler handleComment(XMLStreamReader parser);
	XmlHandler handleEntityReference(XMLStreamReader parser);
	XmlHandler handleAttribute(XMLStreamReader parser);
	XmlHandler handleDTD(XMLStreamReader parser);
	XmlHandler handleCDATA(XMLStreamReader parser);
	XmlHandler handleSPACE(XMLStreamReader parser);

}
