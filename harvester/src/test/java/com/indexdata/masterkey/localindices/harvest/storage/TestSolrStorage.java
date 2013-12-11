package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import javax.xml.stream.XMLStreamException;
import junit.framework.TestCase;
import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.harvest.job.ConsoleStorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.job.StorageJobLogger;

public class TestSolrStorage extends TestCase {
  String solrUrl = "http://localhost:8585/solr/";
  Harvestable harvestable = new DummyXmlBulkResource("dummy", solrUrl);
  
  // Console Storage
  StorageJobLogger logger = new ConsoleStorageJobLogger(TestSolrStorage.class, harvestable);
  
  // Solr Storage
  HarvestStorage storage = new SolrStorage(solrUrl, harvestable) {
    public void commit() throws IOException {
      setWaitSearcher(true);
      super.commit();
    }
    
  };
  String testXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" 
      + "<add>" 
      + "  <doc>\n"
      + "      <field name=\"id\">6690</field>\n"
      + "      <field name=\"author\">Eliot, George, 1819-1880</field>\n"
      + "      <field name=\"author-title\"></field>\n" + "      <field name=\"author-date\"/>\n"
      + "      <field name=\"title\">The Mill on the Floss</field>\n"
      + "      <field name=\"medium\">book</field>\n"
      + "      <field other=\"author-date\">Value</field>\n"
      + "      <other name=\"other\"></other>\n" + "   </doc>\n" + "  <doc>\n"
      + "      <field name=\"id\">6691</field>\n"
      + "      <field name=\"author\">Eliot, George, 1819-1880</field>\n"
      + "      <field name=\"author-title\"></field>\n" + "      <field name=\"author-date\"/>\n"
      + "      <field name=\"title\">The Mill on the Floss</field>\n"
      + "      <field name=\"medium\">book</field>\n"
      + "      <field other=\"author-date\">Value</field>\n"
      + "      <other name=\"other\"></other>\n" 
      + "   </doc>\n" 
      + "</add>";

  public TestSolrStorage() {

  }

  public void testSimplePost() throws IOException {
    SolrXmlHandler context = new SolrXmlHandler();
    SolrXmlParser parser = new SolrXmlParser();
    try {
      parser.parse(testXml, context);
      storage.begin();
      OutputStream output = storage.getOutputStream();
      Writer writer = new OutputStreamWriter(output);
      writer.write(testXml);
      writer.close();
      storage.commit();
    } catch (XMLStreamException e) {
      System.out.println("Failure to parse");
    }
  }
}
