package com.indexdata.masterkey.localindices.harvest.job;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.entity.BasicTransformation;
import com.indexdata.masterkey.localindices.entity.BasicTransformationStep;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.storage.BulkSolrRecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.SolrRecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.StatusNotImplemented;
import com.indexdata.masterkey.localindices.harvest.storage.StorageStatus;

public class TestBulkRecordHarvestJob extends TestCase {

  //String resourceMarc = "http://lui-dev.indexdata.com/ag/demo_org.mrc";
  String resourceMarc = "http://lui-dev.indexdata.com/ag/demo-part-00.mrc";

//  String resourceMarcUTF8 = "http://lui-dev.indexdata.com/oaister/oais.000000.mrc";
  String resourceMarcUTF8gzipped = "http://lui-dev.indexdata.com/oaister/oais.000000.mrc.gz";
  
//  String resourceLoCMarc8gz = "http://lui-dev.indexdata.com/loc/part01.dat.gz";
  String resourceOIAster = "http://maki.indexdata.com/marcdata/meta/oaister/harvester-index.html";
  String resourceMarcGZ = "http://lui-dev.indexdata.com/ag/demo-part-00.mrc.gz";
  String resourceMarcZIP = "http://lui-dev.indexdata.com/ag/demo-part-00.mrc.zip";
  String solrUrl = "http://localhost:8585/solr/";
  RecordStorage recordStorage;

  private XmlBulkResource createResource(String url, String expectedSchema, String outputSchema, int splitAt, int size)
      throws IOException {
    XmlBulkResource resource = new XmlBulkResource(url);
    resource.setName(url + " " + (expectedSchema != null ? expectedSchema + " " : "") + splitAt + " " + size);
    resource.setSplitAt(String.valueOf(splitAt));
    resource.setSplitSize(String.valueOf(size));
    resource.setExpectedSchema(expectedSchema);
    resource.setOutputSchema(outputSchema);
    resource.setEnabled(true);
    resource.setId(1l);
    resource.setCurrentStatus("NEW");
    return resource;
  }
  
  public void TestMarc21TransformationSAX() throws ParserConfigurationException, SAXException, TransformerException {
    testSAXTransformation("resources/marcxml.xml", "resources/marc21.xsl");
  }

  public void TestTurboMarcTransformationSAX() throws ParserConfigurationException, SAXException, TransformerException {
    testSAXTransformation("resources/tmarc.xml", "resources/tmarc.xsl");
  }

  private void testSAXTransformation(String xml, String xsl) throws ParserConfigurationException, SAXException, TransformerException {
    SAXTransformerFactory transformerfactory = (SAXTransformerFactory) TransformerFactory.newInstance(); 
    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    XMLReader reader = parserFactory.newSAXParser().getXMLReader();
    XMLFilter filter = transformerfactory.newXMLFilter(new SAXSource(new InputSource(getClass().getResourceAsStream(xsl))));
    filter.setParent(reader);
    Transformer transformer = transformerfactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    SAXSource xmlSource = new SAXSource(filter, new InputSource(getClass().getResourceAsStream(xml)));
    transformer.transform(xmlSource, new StreamResult(System.out));
  }
  
  private Transformation createTransformationFromResources(String [] steps) throws IOException {
    Transformation transformation = new BasicTransformation();
    int index = 0; 
    for (String resource : steps) {
      	InputStream input = getClass().getResourceAsStream(resource);
      	
      	assertTrue(input != null);
      	byte buf[] = new byte[4096];
      	ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
      	int length = 0;
      	@SuppressWarnings("unused")
	int total = 0;
      	while ((length = input.read(buf)) != -1) { 
      	  byteArray.write(buf, 0, length);
      	  total += length;
      	}
      	//System.out.println("Step " + resource  + " length: " + total );
      	String template = byteArray.toString("UTF-8");
      	TransformationStep step = new BasicTransformationStep("Step " + index, "Test", template);
      	transformation.addStep(step, index++);
    }
    transformation.setId(1l);
    transformation.setName("Test");
    return transformation;
  }

  private RecordHarvestJob doHarvestJob(RecordStorage recordStorage, XmlBulkResource resource)
      throws IOException {
    AbstractRecordHarvestJob job = new BulkRecordHarvestJob(resource, null);
    job.setLogger(new ConsoleStorageJobLogger(job.getClass(), resource));
    job.setStorage(recordStorage);
    job.run();
    return job;
  }

  private Transformation createMarc21Transformation() throws IOException {
    String[] resourceSteps = { "resources/marc21.xsl"};
    return createTransformationFromResources(resourceSteps);
  }

  private Transformation createTurboMarcTransformation() throws IOException {
    String[] resourceSteps = { "resources/tmarc.xsl"};
    return createTransformationFromResources(resourceSteps);
  }

  
  private void testCleanMarc21SplitByNumber(int number) throws IOException, StatusNotImplemented {
    XmlBulkResource resource = createResource(resourceMarc, null, null, 0, number);
    resource.setId(1l);
    resource.setTransformation(createMarc21Transformation());
    
    RecordStorage recordStorage = new SolrRecordStorage(solrUrl, resource);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));
    recordStorage.purge(true);
    StorageStatus storageStatus = recordStorage.getStatus();
    long total = storageStatus.getTotalRecords();
    assertTrue("Total records != 0: " + total, total == 0); 
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);

    storageStatus = recordStorage.getStatus();
    assertTrue(StorageStatus.TransactionState.Committed == storageStatus.getTransactionState());
    assertTrue("Deleted records failed " + storageStatus.getDeletes(), new Long(0).equals(storageStatus.getDeletes()));
    assertTrue("Add records failed " + storageStatus.getAdds(), 	new Long(1002).equals(storageStatus.getAdds()));
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testCleanMarc21NoSplit() throws IOException, StatusNotImplemented {
    testCleanMarc21SplitByNumber(0);
  }

  public void testCleanMarc21Split1() throws IOException, StatusNotImplemented {
    testCleanMarc21SplitByNumber(1);
  }

  public void testCleanMarc21Split100() throws IOException, StatusNotImplemented {
    testCleanMarc21SplitByNumber(100);
  }

  public void testCleanMarc21Split1000() throws IOException, StatusNotImplemented {
    testCleanMarc21SplitByNumber(1000);
  }

  private void testCleanTurboMarcSplitByNumber(int number) throws IOException, StatusNotImplemented {
    XmlBulkResource resource = createResource(resourceMarc, "application/marc", "application/tmarc", 0, number);
    resource.setId(2l);
    resource.setTransformation(createTurboMarcTransformation());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);

    StorageStatus storageStatus = recordStorage.getStatus();
    assertTrue(StorageStatus.TransactionState.Committed == storageStatus.getTransactionState());
    assertTrue("Deleted records failed " + storageStatus.getDeletes(), new Long(0).equals(storageStatus.getDeletes()));
    assertTrue("Add records failed " + storageStatus.getAdds(), 	new Long(1002).equals(storageStatus.getAdds()));
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }


  public void testCleanTurboMarcNoSplit() throws IOException, StatusNotImplemented {
    testCleanTurboMarcSplitByNumber(0);
  }

  public void testCleanTurboMarcSplit1() throws IOException, StatusNotImplemented {
    testCleanTurboMarcSplitByNumber(1);
  }

  public void testCleanTurboMarcSplit100() throws IOException, StatusNotImplemented {
    testCleanTurboMarcSplitByNumber(100);
  }

  public void testCleanTurboMarcSplit1000() throws IOException, StatusNotImplemented {
    testCleanTurboMarcSplitByNumber(1000);
  }

  private void testCleanGZippedMarc21SplitByNumber(int number) throws IOException, StatusNotImplemented {
    XmlBulkResource resource = createResource(resourceMarcGZ, "application/marc", null, 1, number);
    resource.setId(2l);
    resource.setTransformation(createMarc21Transformation());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);

    StorageStatus storageStatus = recordStorage.getStatus();
    assertTrue(StorageStatus.TransactionState.Committed == storageStatus.getTransactionState());
    assertTrue("Deleted records failed " + storageStatus.getDeletes(), new Long(0).equals(storageStatus.getDeletes()));
    assertTrue("Add records failed " + storageStatus.getAdds(), 	new Long(1002).equals(storageStatus.getAdds()));
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testCleanGZippedMarc21NoSplit() throws IOException, StatusNotImplemented {
    testCleanGZippedMarc21SplitByNumber(1);
  }

  public void testCleanGZippedMarc21Split1() throws IOException, StatusNotImplemented {
    testCleanGZippedMarc21SplitByNumber(1);
  }

  public void testCleanGZippedMarc21Split100() throws IOException, StatusNotImplemented {
    testCleanGZippedMarc21SplitByNumber(100);
  }

  public void testCleanGZippedMarc21Split1000() throws IOException, StatusNotImplemented {
    testCleanGZippedMarc21SplitByNumber(1000);
  }

  private void testCleanGZippedTurboMarcSplitByNumber(int number) throws IOException, StatusNotImplemented {
    XmlBulkResource resource = createResource(resourceMarcGZ, "application/marc", "application/tmarc", 1, number);
    resource.setId(2l);
    resource.setTransformation(createTurboMarcTransformation());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);

    StorageStatus storageStatus = recordStorage.getStatus();
    assertTrue(StorageStatus.TransactionState.Committed == storageStatus.getTransactionState());
    assertTrue("Deleted records failed " + storageStatus.getDeletes(), new Long(0).equals(storageStatus.getDeletes()));
    assertTrue("Add records failed " + storageStatus.getAdds(), 	new Long(1002).equals(storageStatus.getAdds()));
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testCleanGZippedTurboMarcNoSplit() throws IOException, StatusNotImplemented {
    testCleanGZippedTurboMarcSplitByNumber(0);
  }

  public void testCleanGZippedTurboMarcSplit1() throws IOException, StatusNotImplemented {
    testCleanGZippedTurboMarcSplitByNumber(1);
  }

  public void testCleanGZippedTurboMarcSplit100() throws IOException, StatusNotImplemented {
    testCleanGZippedTurboMarcSplitByNumber(100);
  }

  public void testCleanGZippedTurboMarcSplit1000() throws IOException, StatusNotImplemented {
    testCleanGZippedTurboMarcSplitByNumber(1000);
  }

  public void testCleanJumpPageGZippedSplitTurboMarc() throws IOException, StatusNotImplemented {
    XmlBulkResource resource = createResource( resourceMarcGZ + " " + resourceMarcGZ, "application/marc", "application/tmarc", 1, 1);
    resource.setId(2l);
    resource.setTransformation(createTurboMarcTransformation());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);

    StorageStatus storageStatus = recordStorage.getStatus();
    assertTrue(StorageStatus.TransactionState.Committed == storageStatus.getTransactionState());
    assertTrue("Deleted records failed " + storageStatus.getDeletes(), new Long(0).equals(storageStatus.getDeletes()));
    assertTrue("Add records failed " + storageStatus.getAdds(), 	new Long(2004).equals(storageStatus.getAdds()));
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  
  /*
  public void testCleanOAIsterSplit1000TurboMarc() throws IOException, StatusNotImplemented {
    XmlBulkResource resource = createResource(resourceMarcUTF8, "application/marc", "application/tmarc", 1, 1000);
    resource.setId(2l);
    resource.setTransformation(createTurboMarcTransformation());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);

    StorageStatus storageStatus = recordStorage.getStatus();
    assertTrue(StorageStatus.TransactionState.Committed == storageStatus.getTransactionState());
    assertTrue("Deleted records failed " + storageStatus.getDeletes(), new Long(0).equals(storageStatus.getDeletes()));
    assertTrue("Add records failed " + storageStatus.getAdds(), 	new Long(100000).equals(storageStatus.getAdds()));
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testCleanOAIsterGZipSplit1000TurboMarc() throws IOException, StatusNotImplemented {
    XmlBulkResource resource = createResource(resourceMarcUTF8, "application/marc", "application/tmarc", 1, 1000);
    resource.setId(2l);
    resource.setTransformation(createTurboMarcTransformation());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);

    StorageStatus storageStatus = recordStorage.getStatus();
    assertTrue(StorageStatus.TransactionState.Committed == storageStatus.getTransactionState());
    assertTrue("Deleted records failed " + storageStatus.getDeletes(), new Long(0).equals(storageStatus.getDeletes()));
    assertTrue("Add records failed " + storageStatus.getAdds(), 	new Long(100000).equals(storageStatus.getAdds()));
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testCleanLoCGzSplit1000TurboMarc() throws IOException, StatusNotImplemented {
    XmlBulkResource resource = createResource(resourceLoCMarc8gz, "application/marc; charset=MARC8", "application/tmarc", 1, 1000);
    resource.setId(2l);
    resource.setTransformation(createTurboMarcTransformation());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);

    StorageStatus storageStatus = recordStorage.getStatus();
    assertTrue(StorageStatus.TransactionState.Committed == storageStatus.getTransactionState());
    assertTrue("Deleted records failed " + storageStatus.getDeletes(), new Long(0).equals(storageStatus.getDeletes()));
    assertTrue("Add records failed " + storageStatus.getAdds(), 	new Long(250000).equals(storageStatus.getAdds()));
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }
  */
}
