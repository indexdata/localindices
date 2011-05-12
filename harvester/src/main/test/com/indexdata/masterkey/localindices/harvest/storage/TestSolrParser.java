package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.stream.XMLStreamException;

import org.apache.solr.common.SolrInputDocument;

import junit.framework.TestCase;

public class TestSolrParser extends TestCase 
{
	String xml 
		  = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
			"<add>" +
			"  <doc>\n" + 
			"      <field name=\"id\">6688</field>\n" + 
			"      <field name=\"author\">Eliot, George, 1819-1880</field>\n" + 
			"      <field name=\"author-title\"></field>\n" + 
			"      <field name=\"author-date\"/>\n" + 
			"      <field name=\"title\">The Mill on the Floss</field>\n" + 
			"      <field name=\"medium\">book</field>\n" +
			"      <field other=\"author-date\">Value</field>\n" + 
			"      <other name=\"other\"></other>\n" +
			"   </doc>\n" + 
			"  <doc>\n" + 
			"      <field name=\"id\">6689</field>\n" +
			"      <field name=\"author\">Eliot, George, 1819-1880</field>\n" + 
			"      <field name=\"author-title\"></field>\n" + 
			"      <field name=\"author-date\"/>\n" + 
			"      <field name=\"title\">The Mill on the Floss</field>\n" + 
			"      <field name=\"medium\">book</field>\n" +
			"      <field other=\"author-date\">Value</field>\n" + 
			"      <other name=\"other\"></other>\n" +
			"   </doc>\n" + 
			"</add>";
	
	public void testSolrParse() throws IOException 
	{
		SolrXmlParser parser = new SolrXmlParser();
		XmlHandler context = new PrintXmlHandler();
		try {
			parser.parse(xml, context);
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}

		SolrXmlHandler solrContext = new SolrXmlHandler();
		try {
			parser.parse(xml, solrContext);
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		Collection<SolrInputDocument> documents = solrContext.getDocuments();
		assertTrue(documents.size() == 2); 
		Iterator<SolrInputDocument> iterator = documents.iterator();
		while (iterator.hasNext()) {
			SolrInputDocument doc = iterator.next();
			System.out.print(doc.entrySet());
		}
		try {
			parser.parse(xml, solrContext);
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		// Parsed again, updated but same count
		assertTrue("Document size is not 4: " + documents.size(), documents.size() == 4); 
		iterator = documents.iterator();
		while (iterator.hasNext()) {
			SolrInputDocument doc = iterator.next();
			System.out.print(doc.entrySet());
		}
	}


}
