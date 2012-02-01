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

public class TestBulkRecordHarvestJob extends TestCase {

  //String resourceMarc = "http://lui.indexdata.com/ag/demo_org.mrc";
  String resourceMarc = "http://lui.indexdata.com/ag/demo-part-00.mrc";

  String resourceMarcGZ = "http://lui.indexdata.com/ag/demo-part-00.mrc.gz";
  String resourceMarcZIP = "http://lui.indexdata.com/ag/demo-part-00.mrc.zip";
  String solrUrl = "http://localhost:8080/solr/";
  RecordStorage recordStorage;

  private XmlBulkResource createResource(String url, String expectedSchema, int splitAt, int size)
      throws IOException {
    XmlBulkResource resource = new XmlBulkResource(url);
    resource.setName(url + " " + (expectedSchema != null ? expectedSchema : "") + splitAt + " " + size);
    resource.setSplitAt(String.valueOf(splitAt));
    resource.setSplitSize(String.valueOf(size));
    resource.setExpectedSchema(expectedSchema);
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
      	int total = 0;
      	while ((length = input.read(buf)) != -1) { 
      	  byteArray.write(buf, 0, length);
      	  total += length;
      	}
      	System.out.println("Step " + resource  + " length: " + total );
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
    RecordHarvestJob job = new BulkRecordHarvestJob(resource, null);
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

  
  public void testCleanNoSplit() throws IOException {
    XmlBulkResource resource = createResource(resourceMarc, null, 0, 0);
    resource.setId(1l);
    resource.setTransformation(createMarc21Transformation());
    
    RecordStorage recordStorage = new SolrRecordStorage(solrUrl, resource);
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testCleanSplit1000BulkHarvestJob() throws IOException {
    XmlBulkResource resource = createResource(resourceMarc, null, 1, 1000);
    resource.setTransformation(createMarc21Transformation());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testCleanTurboMarcHarvestJob() throws IOException {
    XmlBulkResource resource = createResource(resourceMarc, "application/tmarc", 0, 0);
    resource.setId(2l);
    resource.setTransformation(createTurboMarcTransformation());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testCleanTurboMarcSplitHarvestJob() throws IOException {
    XmlBulkResource resource = createResource(resourceMarc, "application/tmarc", 1, 1000);
    resource.setId(2l);
    resource.setTransformation(createTurboMarcTransformation());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);    
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testCleanGZippedSplitOneMarc21() throws IOException {
    XmlBulkResource resource = createResource(resourceMarcGZ, "application/marc", 1, 1);
    resource.setId(2l);
    resource.setTransformation(createMarc21Transformation());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testCleanGZippedSplitTurboMarc() throws IOException {
    XmlBulkResource resource = createResource(resourceMarc, "application/tmarc", 1, 1000);
    resource.setId(2l);
    resource.setTransformation(createTurboMarcTransformation());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }


}
