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
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.entity.WebCrawlResource;
import com.indexdata.masterkey.localindices.entity.XmlTransformationStep;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;

public class TestWebRecordHarvestJob extends TestCase {

  //String resourceMarc = "http://lui-dev.indexdata.com/ag/demo_org.mrc";
  String resourceWeb = "http://www.indexdata.com";
  String solrUrl = "http://localhost:8585/solr/";
  RecordStorage recordStorage;

  @SuppressWarnings("unused")
  private WebCrawlResource createResource(String url)
      throws IOException {
    WebCrawlResource resource = new WebCrawlResource();
    resource.setStartUrls(url);
    resource.setName(url);
    resource.setEnabled(true);
    resource.setId(1l);
    resource.setCurrentStatus("NEW");
    resource.setRecursionDepth(2);
    
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
      	TransformationStep step = new XmlTransformationStep("Step " + index, "Test", template);
      	transformation.addStep(step, index++);
    }
    transformation.setId(1l);
    transformation.setName("Test");
    return transformation;
  }

  @SuppressWarnings("unused")
  private RecordHarvestJob doHarvestJob(RecordStorage recordStorage, WebCrawlResource resource)
      throws IOException {
    WebRecordHarvestJob job = new WebRecordHarvestJob(resource, null);
    job.setLogger(new ConsoleStorageJobLogger(job.getClass(), resource));
    job.setStorage(recordStorage);
    job.run();
    return job;
  }

  @SuppressWarnings("unused")
  private Transformation createMarc21Transformation() throws IOException {
    String[] resourceSteps = { "resources/marc21.xsl"};
    return createTransformationFromResources(resourceSteps);
  }

  /*
  public void testCleanNoSplit() throws IOException, StatusNotImplemented {
    WebCrawlResource resource = createResource(resourceWeb);
    resource.setId(1l);
    resource.setTransformation(null);
    FileStorageEntity fileStorageEntity = new FileStorageEntity();
    fileStorageEntity.setName("Test");
    fileStorageEntity.setUrl("file://test.txt");
    fileStorageEntity.setId(1l);
    
    resource.setStorage(fileStorageEntity);
    
    BulkSolrRecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    //recordStorage.setHarvestable(resource);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));
    recordStorage.begin();
    recordStorage.purge(true);
    recordStorage.commit();
    StorageStatus storageStatus = recordStorage.getStatus();
    assertTrue("Total records != 0", storageStatus.getTotalRecords() == 0); 
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);

    storageStatus = recordStorage.getStatus();
    assertTrue(StorageStatus.TransactionState.Committed == storageStatus.getTransactionState());
    assertTrue("Deleted records failed " + storageStatus.getDeletes(), new Long(0).equals(storageStatus.getDeletes()));
    assertTrue("Add records failed " + storageStatus.getAdds(), new Long(16).equals(storageStatus.getAdds()));
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }
*/
}
