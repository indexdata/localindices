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

  private static final int NO_RECORDS = 1002;
  //String resourceMarc = "http://lui-dev.indexdata.com/ag/demo_org.mrc";
  long records_in_marc = 1002;
  String resourceMarc0 = "http://lui-dev.indexdata.com/loc/loc-small.0000000.xml";
  String resourceMarc1 = "http://lui-dev.indexdata.com/loc/loc-small.0000001.xml";
  String resourceMarc2 = "http://lui-dev.indexdata.com/loc/loc-small.0000002.xml";
  String resourceTurboMarc0 = "http://lui-dev.indexdata.com/loc/loc-small.0000000.txml";

//  String resourceMarcUTF8 = "http://lui-dev.indexdata.com/oaister/oais.000000.mrc";
  String resourceMarcUTF8gzipped = "http://lui-dev.indexdata.com/oaister/oais.000000.mrc.gz";
  
//  String resourceLoCMarc8gz = "http://lui-dev.indexdata.com/loc/part01.dat.gz";
  String resourceOIAster = "http://maki.indexdata.com/marcdata/meta/oaister/harvester-index.html";
  String resourceMarcGZ = "http://lui-dev.indexdata.com/ag/demo-part-00.mrc.gz";
  String resourceMarcZIP = "http://lui-dev.indexdata.com/ag/demo-part-00.mrc.zip";
  String solrUrl = "http://localhost:8585/solr/";
  RecordStorage recordStorage;

  private XmlBulkResource createResource(String url, String expectedSchema, String outputSchema, int splitAt, 
      	int size, boolean overwrite)
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
    resource.setOverwrite(overwrite);
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

  private void testMarc21SplitByNumber(int number, boolean clear, boolean overwrite, long expected_total) throws IOException, StatusNotImplemented {
    XmlBulkResource resource = createResource(resourceMarc0, null, null, 0, number, overwrite);
    resource.setId(1l);
    resource.setTransformation(createMarc21Transformation());
    
    RecordStorage recordStorage = new SolrRecordStorage(solrUrl, resource);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));
    if (clear) { 
      purgeStorage(recordStorage);
    }
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    assertTrue("Job not finished: " + job.getStatus(), job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), NO_RECORDS, 0, NO_RECORDS);
  }

  private void purgeStorage(RecordStorage recordStorage) throws IOException, StatusNotImplemented {
    recordStorage.purge(true);
    StorageStatus storageStatus = recordStorage.getStatus();
    long total = storageStatus.getTotalRecords();
    assertTrue("Total records != 0: " + total, total == 0);
  }

  private void testCleanMarc21SplitByNumber(int number) throws IOException, StatusNotImplemented {
    testMarc21SplitByNumber(number, true, true, NO_RECORDS);
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

  private void testCleanTurboMarcSplitByNumber(int number, boolean clean, boolean overwrite, 
      long expected_total) throws IOException, StatusNotImplemented {
    XmlBulkResource resource = createResource(resourceTurboMarc0, "application/marc", "application/tmarc", 0, number, overwrite);
    resource.setId(2l);
    resource.setTransformation(createTurboMarcTransformation());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));
    if (clean)
      purgeStorage(recordStorage);
    
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), NO_RECORDS, 0, expected_total);
  }


  public void testCleanTurboMarcNoSplit() throws IOException, StatusNotImplemented {
    testCleanTurboMarcSplitByNumber(0, true, true, NO_RECORDS);
  }

  public void testCleanTurboMarcSplit1() throws IOException, StatusNotImplemented {
    testCleanTurboMarcSplitByNumber(1, true, true, NO_RECORDS);
  }

  public void testCleanTurboMarcSplit100() throws IOException, StatusNotImplemented {
    testCleanTurboMarcSplitByNumber(100, true, true, NO_RECORDS);
  }

  public void testCleanTurboMarcSplit1000() throws IOException, StatusNotImplemented {
    testCleanTurboMarcSplitByNumber(1000, true, true, NO_RECORDS);
  }

  private void testGZippedMarc21SplitByNumber(int number, boolean clean, boolean overwrite, long total_expected) throws IOException, StatusNotImplemented {
    XmlBulkResource resource = createResource(resourceMarcGZ, "application/marc", null, 1, number, overwrite);
    resource.setId(2l);
    resource.setTransformation(createMarc21Transformation());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));
    if (clean) 
      purgeStorage(recordStorage);
      
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    
    checkStorageStatus(recordStorage.getStatus(), NO_RECORDS, 0, NO_RECORDS);
  }

  private void testCleanGZippedMarc21SplitByNumber(int number, boolean overwrite, 
      long expected_total) throws IOException, StatusNotImplemented {
    testGZippedMarc21SplitByNumber(number, true, overwrite, NO_RECORDS);
  }

  public void testCleanGZippedMarc21NoSplit() throws IOException, StatusNotImplemented {
    testCleanGZippedMarc21SplitByNumber(1, true, NO_RECORDS);
  }

  public void testCleanGZippedMarc21Split1() throws IOException, StatusNotImplemented {
    testCleanGZippedMarc21SplitByNumber(1, true, NO_RECORDS);
  }

  public void testCleanGZippedMarc21Split100() throws IOException, StatusNotImplemented {
    testCleanGZippedMarc21SplitByNumber(100, true, NO_RECORDS);
  }

  public void testCleanGZippedMarc21Split1000() throws IOException, StatusNotImplemented {
    testCleanGZippedMarc21SplitByNumber(1000, true, NO_RECORDS);
  }

  private void testGZippedTurboMarcSplitByNumber(int number, boolean clear, boolean overwrite, 
      long expected_total) throws IOException, StatusNotImplemented {
    XmlBulkResource resource = createResource(resourceMarcGZ, "application/marc", "application/tmarc", 1, 
	number, overwrite);
    resource.setId(2l);
    resource.setTransformation(createTurboMarcTransformation());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));

    if (clear) 
      recordStorage.purge(true);

    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), NO_RECORDS, 0, expected_total);
  }

  private void testCleanGZippedTurboMarcSplitByNumber(int number) throws IOException, StatusNotImplemented {
    testGZippedTurboMarcSplitByNumber(number, true, true, NO_RECORDS);
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

  public void testJumpPageGZippedTurboMarcSplitBy(int number, boolean clear, boolean overwrite, long expected_total) throws IOException, StatusNotImplemented {
    XmlBulkResource resource = createResource( resourceMarc0 + " " + resourceMarc1, "application/marc", "application/tmarc", 1, number, overwrite);
    resource.setId(2l);
    resource.setTransformation(createTurboMarcTransformation());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));
    if (clear)
      purgeStorage(recordStorage);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    assertTrue("Job not finished: " + job.getStatus(), job.getStatus() == HarvestStatus.FINISHED);

    checkStorageStatus(recordStorage.getStatus(), 2 * NO_RECORDS, 0, 2 * NO_RECORDS);
  }

  private void checkStorageStatus(StorageStatus storageStatus, long add, long delete, long total) {
    assertTrue(StorageStatus.TransactionState.Committed == storageStatus.getTransactionState());
    assertTrue("Deleted records failed " + storageStatus.getDeletes(), 
		new Long(delete).equals(storageStatus.getDeletes()));
    assertTrue("Add records failed " + storageStatus.getAdds(), 
		new Long(add).equals(storageStatus.getAdds()));
    assertTrue("Total records failed" + storageStatus.getTotalRecords(), 
		new Long(total).equals(storageStatus.getTotalRecords()));
  }

  public void testCleanJumpPageGZippedTurboMarcSplitBy(int number, boolean clear, boolean overwrite, 
      long expected_total) throws IOException, StatusNotImplemented {
    testJumpPageGZippedTurboMarcSplitBy(1, true, true, 1002); 
  }

  
  /*
  public void testCleanOAIsterSplit1000TurboMarc() throws IOException, StatusNotImplemented {
    XmlBulkResource resource = createResource(resourceMarcUTF8, "application/marc", "application/tmarc", 1, 1000);
    resource.setId(2l);
    resource.setTransformation(createTurboMarcTransformation());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));
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
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);

    StorageStatus storageStatus = recordStorage.getStatus();
    assertTrue(StorageStatus.TransactionState.Committed == storageStatus.getTransactionState());
    assertTrue("Deleted records failed " + storageStatus.getDeletes(), new Long(0).equals(storageStatus.getDeletes()));
    assertTrue("Add records failed " + storageStatus.getAdds(), 	new Long(250000).equals(storageStatus.getAdds()));
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }
  */
}
