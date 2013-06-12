package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.harvest.job.BulkRecordHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.ConsoleStorageJobLogger;
import com.indexdata.masterkey.localindices.harvest.job.HarvestStatus;
import com.indexdata.masterkey.localindices.harvest.job.OAIHarvestJob;
import com.indexdata.masterkey.localindices.harvest.job.RecordHarvestJob;
import com.indexdata.xml.factory.XmlFactory;

public class TestTransformationChainStorage extends TestCase {
  DummyXmlBulkResource harvestableXml = new DummyXmlBulkResource(
      "http://lui-dev.indexdata.com/harvester/marc.xml");

  String catalog_gz = "http://lui-dev.indexdata.com/gutenberg/catalog.rdf.gz";
  Harvestable harvestableGutenberg = new DummyXmlBulkResource(catalog_gz);

  String marcRecords = "http://lui.indexdata.com/ag/demo_org.mrc";
  Harvestable harvestableMarc = new DummyXmlBulkResource(marcRecords);

  String catalog_zip = "http://www.gutenberg.org/feeds/catalog.rdf.zip";

  // SOLR Server in container
  String solrUrl = "http://localhost:8585/solr/";
  SolrStorage solrStorage = new SolrStorage(solrUrl, harvestableXml);
  RecordStorage xmlRecordStorage = new SolrRecordStorage(solrUrl, harvestableXml);
  RecordStorage xmlBulkStorage = new BulkSolrRecordStorage(solrUrl, harvestableXml);
  
  RecordStorage bulkGutenbergStorage = new BulkSolrRecordStorage(solrUrl, harvestableGutenberg);
  RecordStorage bulkMarcStorage = new BulkSolrRecordStorage(solrUrl, harvestableMarc);
  SAXParserFactory spf = XmlFactory.newSAXParserFactoryInstance();
  SAXTransformerFactory stf = (SAXTransformerFactory) XmlFactory.newTransformerInstance();

  public TestTransformationChainStorage() {
    harvestableXml.setId(1l);
    xmlRecordStorage.setLogger(new ConsoleStorageJobLogger(xmlRecordStorage.getClass(), harvestableXml.getStorage()));
    xmlBulkStorage.setLogger(new ConsoleStorageJobLogger(xmlBulkStorage.getClass(), harvestableXml.getStorage()));
  }

  public void testTransformationChain_GZIP_Download() throws IOException,
      TransformerConfigurationException, ParserConfigurationException, SAXException {

    URL url = new URL(catalog_gz);
    HttpURLConnection conn = null;
    conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    int responseCode = conn.getResponseCode();
    assertTrue("responseCode: " + responseCode, responseCode == 200);
    int contentLength = conn.getContentLength();
    //String contentType = conn.getContentType();
    int total = 0;
    if (responseCode == 200) {
      InputStream input = new GZIPInputStream(conn.getInputStream());
      byte[] buffer = new byte[4092];
      while (true) {
	int length = input.read(buffer);
	if (length < 0)
	  break;
	total += length;
      }
      assertTrue("Length is wrong: " + contentLength + "<= " + total, contentLength <= total);
      return ;
    }
  }

  public void testTransformationChain_ZIP_Download() throws IOException,
      TransformerConfigurationException, ParserConfigurationException, SAXException {

    URL url = new URL(catalog_zip);
    HttpURLConnection conn = null;
    conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    int responseCode = conn.getResponseCode();
    assertTrue("responseCode: " + responseCode, responseCode == 200);
    int contentLength = conn.getContentLength();
    //String contentType = conn.getContentType();
    int total = 0;
    if (responseCode == 200) {
      ZipInputStream zipInput = new ZipInputStream(conn.getInputStream());
      ZipEntry entry = zipInput.getNextEntry();
      InputStream input = zipInput;
      while (entry != null) {
	byte[] buffer = new byte[10240];
	while (true) {
	  int length = input.read(buffer);
	  if (length < 0)
	    break;
	  total += length;
	}
	entry = zipInput.getNextEntry();
      }
      zipInput.close();
      assertTrue("Length is wrong: " + contentLength + "<= " + total, contentLength <= total);
    }
  }

  
  public void testSimpleTransformationStorage() throws IOException,
      TransformerConfigurationException, ParserConfigurationException, SAXException {
    String[] stylesheets = { pz2solr_xsl };
    XMLReader xmlReader = createTransformChain(stylesheets);
    TransformationChainStorageProxy transformStorage = new TransformationChainStorageProxy(
	solrStorage, xmlReader);
    transformStorage.begin();
    OutputStream output = transformStorage.getOutputStream();
    Writer writer = new OutputStreamWriter(output);
    writer.write(locPzXml);
    writer.close();
    transformStorage.commit();
  }

  public void testTransformationChain_OAI_PMH_DC_to_PZ_to_SolrStorage() throws IOException,
      TransformerConfigurationException, ParserConfigurationException, SAXException {
    String[] stylesheets = { oaidc_pmh_xsl, pz2solr_xsl };
    System.out.println("testTransformationChain_OAI_PMH_DC_to_PZ_to_SolrStorage");
    XMLReader xmlReader = createTransformChain(stylesheets);
    HarvestStorage transformStorage = new TransformationChainStorageProxy(solrStorage, xmlReader);
    transformStorage.begin();
    OutputStream output = transformStorage.getOutputStream();
    //String oai_pmh_oaidc = FileUtils.readFileToString(FileUtils.toFile(this.getClass().getResource("resources/oaipmh_dc.xml")));

    Writer writer = new OutputStreamWriter(output);
    //File dump = new File("oai_pmh_oaidc.xml");
    //OutputStream out = new FileOutputStream(dump);
    //out.write(oai_pmh_oaidc.getBytes());
    //out.close();
    writer.write(oai_pmh_oaidc);
    writer.close();
    transformStorage.commit();
  }

  public void testTransformationChain_OAI_PMH_MARC_to_PZ_to_SolrStorage() throws IOException,
      TransformerConfigurationException, ParserConfigurationException, SAXException {
    String[] stylesheets = { oai2marc_xsl, marc21_xsl, pz2solr_xsl };
    XMLReader xmlReader = createTransformChain(stylesheets);
    System.out.println("testTransformationChain_OAI_PMH_MARC_to_PZ_to_SolrStorage");
    TransformationChainStorageProxy transformStorage = new TransformationChainStorageProxy(
	solrStorage, xmlReader);
    transformStorage.begin();
    OutputStream output = transformStorage.getOutputStream();
    Writer writer = new OutputStreamWriter(output);
    writer.write(oai_pmh_marcxml);
    writer.close();
    transformStorage.commit();
  }
  
   public void testTransformationChain_OAI_PMH_MARC_to_PZ_to_BulkRecordSolrStorage()
      throws IOException, TransformerConfigurationException, ParserConfigurationException,
      SAXException {
    String[] stylesheets = { oai2marc_xsl, marc21_xsl, pz2solr_xsl };
    System.out.println("testTransformationChain_OAI_PMH_MARC_to_PZ_to_BulkRecordSolrStorage");
    harvestableXml.setId(1l);
    
    // harvestable.setTransformation(transformation)
    XMLReader xmlReader = createTransformChain(stylesheets);
    BulkRecordHarvestJob job = new BulkRecordHarvestJob(harvestableXml, null);
    job.setStatus(HarvestStatus.NEW);
    xmlBulkStorage.setLogger(new ConsoleStorageJobLogger(xmlRecordStorage.getClass(), harvestableXml));
    TransformationChainRecordStorageProxy transformStorage = new TransformationChainRecordStorageProxy(
	xmlBulkStorage, xmlReader, job);
    transformStorage.begin();
    OutputStream output = transformStorage.getOutputStream();
    Writer writer = new OutputStreamWriter(output);
    writer.write(oai_pmh_marcxml);
    writer.close();
    transformStorage.commit();
  }

  public XMLReader createTransformChain(String[] stylesheets) throws ParserConfigurationException,
      SAXException, TransformerConfigurationException, UnsupportedEncodingException {
    // Set up to read the input file

    SAXParser parser = spf.newSAXParser();
    XMLReader reader = parser.getXMLReader();

    XMLFilter filter;
    XMLReader parent = reader;
    int index = 0;
    while (index < stylesheets.length) {
      filter = stf.newXMLFilter(new StreamSource(new ByteArrayInputStream(stylesheets[index]
	  .getBytes("UTF-8"))));
      filter.setParent(parent);
      parent = filter;
      index++;
    }
    return parent;
  }

  public void testDOM2SAXRecordStorage() throws TransformerException, ParserConfigurationException,
      SAXException, IOException {
    try {
      Transformer transfomer = stf.newTransformer();
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(new ByteArrayInputStream(oai_pmh_oaidc.getBytes("UTF-8")));
      xmlRecordStorage.setLogger(new ConsoleStorageJobLogger(xmlRecordStorage.getClass(), harvestableXml));
      OaiPmhDcContentHandler contentHandler = new OaiPmhDcContentHandler(xmlRecordStorage, "test");
      Result result = new SAXResult(contentHandler);
      transfomer.transform(new DOMSource(doc), result);
      xmlRecordStorage.commit();
    } catch (TransformerConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void testDOM2SAXBulkRecordStorage() throws TransformerException,
      ParserConfigurationException, SAXException, IOException {
    try {
      Transformer transfomer = stf.newTransformer();
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(new ByteArrayInputStream(oai_pmh_oaidc.getBytes("UTF-8")));
      xmlBulkStorage.setLogger(new ConsoleStorageJobLogger(xmlBulkStorage.getClass(), harvestableXml));
      OaiPmhDcContentHandler contentHandler = new OaiPmhDcContentHandler(xmlBulkStorage, "test");
      Result result = new SAXResult(contentHandler);
      transfomer.transform(new DOMSource(doc), result);
      xmlBulkStorage.commit();
    } catch (TransformerConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  String resourceUrl = "http://ir.ub.rug.nl/oai/";

  public void TestOutputStreamToRecordHarvestJob() throws IOException,
      TransformerConfigurationException, ParserConfigurationException, SAXException {
    String[] stylesheets = { oaidc_pmh_xsl };
    OaiPmhResource resource = new OaiPmhResource();
    resource.setEnabled(true);
    // Approx X days back
    Date fromDate = new Date(new Date().getTime() - 1000 * 60 * 60 * 24 * 10l);
    resource.setFromDate(fromDate);
    resource.setMetadataPrefix("oai_dc");
    resource.setUrl(resourceUrl);
    setName(resourceUrl);
    resource.setCurrentStatus("NEW");
    OAIHarvestJob job = new OAIHarvestJob(resource, null);
    BulkSolrRecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));
    XMLReader xmlFilter = createTransformChain(stylesheets);
    TransformationChainRecordStorageProxy storageProxy = new TransformationChainRecordStorageProxy(
	recordStorage, xmlFilter, null);
    // Clean database completely before run
    storageProxy.purge(true);
    // TODO Check empty database
    job.setStorage(storageProxy);
    job.run();
    // TODO Check records in storage
  }
  
  public void testReadingMultipleXmlFiles() {
    
  }

  String locPzXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
      + "<pz:collection xmlns:pz=\"http://www.indexdata.com/pazpar2/1.0\" >\n"
      + "<pz:record mergekey=\"title Adapting to Web standards : author  medium electronic resource\">\n"
      + "  <pz:metadata type=\"id\">15173151</pz:metadata>\n"
      + "  <pz:metadata type=\"lccn\">  2008270541</pz:metadata>\n"
      + "  <pz:metadata type=\"isbn\">0321501829 (pbk.)</pz:metadata>\n"
      + "  <pz:metadata type=\"isbn\">9780321501820 (pbk.)</pz:metadata>\n"
      + "  <pz:metadata type=\"date\">c2008.</pz:metadata>\n"
      + "  <pz:metadata type=\"title\">Adapting to Web standards :</pz:metadata>\n"
      + "  <pz:metadata type=\"title-remainder\">CSS and Ajax for big sites /</pz:metadata>\n"
      + "  <pz:metadata type=\"title-responsibility\">Christopher Schmitt ... [et al.].</pz:metadata>\n"
      + "  <pz:metadata type=\"title-dates\"/>\n"
      + "  <pz:metadata type=\"title-medium\"/>\n"
      + "  <pz:metadata type=\"title-number-section\"/>\n"
      + "  <pz:metadata type=\"publication-place\">Berkeley, Calif. :</pz:metadata>\n"
      + "  <pz:metadata type=\"publication-name\">New Riders,</pz:metadata>\n"
      + "  <pz:metadata type=\"publication-date\">c2008.</pz:metadata>\n"
      + "  <pz:metadata type=\"physical-extent\">ix, 280 p. :</pz:metadata>\n"
      + "  <pz:metadata type=\"physical-format\">col. ill.</pz:metadata>\n"
      + "  <pz:metadata type=\"physical-dimensions\">23 cm.</pz:metadata>\n"
      + "  <pz:metadata type=\"physical-accomp\"/>\n"
      + "  <pz:metadata type=\"physical-unittype\"/>\n"
      + "  <pz:metadata type=\"physical-unitsize\"/>\n"
      + "  <pz:metadata type=\"physical-specified\"/>\n"
      + "  <pz:metadata type=\"series-title\">Voices that matter</pz:metadata>\n"
      + "  <pz:metadata type=\"description\">Includes index.</pz:metadata>\n"
      + "  <pz:metadata type=\"subject\">Cascading style sheets.</pz:metadata>\n"
      + "  <pz:metadata type=\"subject-long\">Cascading style sheets.</pz:metadata>\n"
      + "  <pz:metadata type=\"subject\">Web sites</pz:metadata>\n"
      + "  <pz:metadata type=\"subject-long\">Web sites, Design, Handbooks, manuals, etc.</pz:metadata>\n"
      + "  <pz:metadata type=\"subject\">Web site development</pz:metadata>\n"
      + "  <pz:metadata type=\"subject-long\">Web site development, Handbooks, manuals, etc.</pz:metadata>\n"
      + "  <pz:metadata type=\"subject\">Ajax (Web site development technology)</pz:metadata>\n"
      + "  <pz:metadata type=\"subject-long\">Ajax (Web site development technology)</pz:metadata>\n"
      + "  <pz:metadata type=\"electronic-url\">http://www.loc.gov/catdir/toc/fy0803/2008270541.html</pz:metadata>\n"
      + "  <pz:metadata type=\"electronic-text\"/>\n"
      + "  <pz:metadata type=\"electronic-note\"/>\n"
      + "  <pz:metadata type=\"medium\">electronic resource</pz:metadata>\n"
      + "  <pz:metadata tag=\"tag100\" />\n" + "</pz:record>" +
      /*
       * "  <pz:record mergekey=\"title The not-just-anybody family. author Byars, Betsy Cromer. medium book\">\n"
       * + "    <pz:metadata type=\"id\">70307</pz:metadata>\n" +
       * "    <pz:metadata type=\"isbn\">0440459516</pz:metadata>\n" +
       * "    <pz:metadata type=\"author\">Byars, Betsy Cromer.</pz:metadata>\n"
       * + "    <pz:metadata type=\"author-title\"/>\n" +
       * "    <pz:metadata type=\"author-date\"/>\n" +
       * "    <pz:metadata type=\"title\">The not-just-anybody family.</pz:metadata>\n"
       * + "    <pz:metadata type=\"title-remainder\"/>\n" +
       * "    <pz:metadata type=\"title-responsibility\"/>\n" +
       * "    <pz:metadata type=\"title-dates\"/>\n" +
       * "    <pz:metadata type=\"title-medium\"/>\n" +
       * "    <pz:metadata type=\"title-number-section\"/>\n" +
       * "    <pz:metadata type=\"medium\">book</pz:metadata>\n" +
       * "    <pz:metadata type=\"meta-marc-tags\">020 090 100 245 942 952 </pz:metadata>\n"
       * +
       * "    <pz:metadata type=\"meta-marc-cf008\">950323s19uu           c      000 1 eng d</pz:metadata>\n"
       * +
       * "    <pz:metadata type=\"meta-frbr-short-title\">The not-just-anybody family.</pz:metadata>\n"
       * +
       * "    <pz:metadata type=\"meta-frbr-full-title\">The not-just-anybody family.</pz:metadata>\n"
       * + "    <pz:metadata type=\"meta-frbr-lang\">eng</pz:metadata>\n" +
       * "  </pz:record>" +
       */
      "</pz:collection>";

  String marcxml = "<marc:record>\n"
      + "  <marc:leader>00428nam a2200157   4500</marc:leader>\n"
      + "  <marc:controlfield tag=\"001\">70307</marc:controlfield>\n"
      + "  <marc:controlfield tag=\"003\">ACLS</marc:controlfield>\n"
      + "  <marc:controlfield tag=\"005\">20000817000000.0</marc:controlfield>\n"
      + "  <marc:controlfield tag=\"007\">ta</marc:controlfield>\n"
      + "  <marc:controlfield tag=\"008\">950323s19uu           c      000 1 eng d</marc:controlfield>\n"
      + "  <marc:datafield tag=\"020\" ind1=\" \" ind2=\" \">\n"
      + "    <marc:subfield code=\"a\">0440459516</marc:subfield>\n" + "  </marc:datafield>\n"
      + "  <marc:datafield tag=\"090\" ind1=\" \" ind2=\" \">\n"
      + "    <marc:subfield code=\"c\">2</marc:subfield>\n"
      + "    <marc:subfield code=\"d\">2</marc:subfield>\n" + "  </marc:datafield>\n"
      + "  <marc:datafield tag=\"100\" ind1=\"1\" ind2=\" \">\n"
      + "    <marc:subfield code=\"a\">Byars, Betsy Cromer.</marc:subfield>\n"
      + "  </marc:datafield>\n" + "  <marc:datafield tag=\"245\" ind1=\"1\" ind2=\"4\">\n"
      + "    <marc:subfield code=\"a\">The not-just-anybody family.</marc:subfield>\n"
      + "  </marc:datafield>\n" + "  <marc:datafield tag=\"942\" ind1=\" \" ind2=\" \">\n"
      + "    <marc:subfield code=\"a\">ONe</marc:subfield>\n"
      + "    <marc:subfield code=\"c\">JF</marc:subfield>\n"
      + "    <marc:subfield code=\"k\">J Byars</marc:subfield>\n"
      + "    <marc:subfield code=\"d\">CIRC</marc:subfield>\n" + "  </marc:datafield>\n"
      + "  <marc:datafield tag=\"952\" ind1=\" \" ind2=\" \">\n"
      + "    <marc:subfield code=\"2\">0000-00-00</marc:subfield>\n"
      + "    <marc:subfield code=\"b\">ALB</marc:subfield>\n"
      + "    <marc:subfield code=\"d\">ALB</marc:subfield>\n"
      + "    <marc:subfield code=\"w\">2003-09-01</marc:subfield>\n"
      + "    <marc:subfield code=\"u\">2</marc:subfield>\n"
      + "    <marc:subfield code=\"k\">J Byars</marc:subfield>\n"
      + "    <marc:subfield code=\"p\">37000000012023</marc:subfield>\n"
      + "    <marc:subfield code=\"v\">2003-09-01</marc:subfield>\n"
      + "    <marc:subfield code=\"x\">2001-11-27</marc:subfield>\n" + "  </marc:datafield>\n"
      + "</marc:record>";

  String oai_pmh_records_start = "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\"\n "
      + "         xmlns:oai=\"http://www.openarchives.org/OAI/2.0/\"\n "
      + "	      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
      + "    	  xmlns:marc=\"http://www.loc.gov/MARC21/slim\"\n"
      + "	      xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/\n"
      + "		  http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n"
      + "	<responseDate>2011-05-03T06:42:57Z</responseDate>\n"
      + "	<request verb=\"ListRecords\" from=\"2010-01-01\" metadataPrefix=\"marcxml\">http://ijict.org/index.php/ijoat/oai</request>\n"
      + "	<ListRecords>\n";

  String record_add = "		<record>\n" + "			<header>\n"
      + "				<identifier>oai:ojs.ijict.org:article/156</identifier>\n"
      + "				<datestamp>2010-10-11T05:39:24Z</datestamp>\n" + "				<setSpec>ijoat:EA</setSpec>\n"
      + "			</header>" + "           <metadata>";

  String record_delete = "		<record status=\"deleted\">\n" + "			<header>\n"
      + "				<identifier>oai:ojs.ijict.org:article/156</identifier>\n"
      + "				<datestamp>2010-10-11T05:39:24Z</datestamp>\n" + "				<setSpec>ijoat:EA</setSpec>\n"
      + "			</header>" + "           <metadata>";

  String record_end = "			</metadata>\n" + "		</record>\n";

  String oai_pmh_end_record = "	</ListRecords>\n" + "</OAI-PMH>";

  /* 
   * This data contains UTF-8 character (’) on purpose. To get this working on non-UTF-8 systems like Mac OS, 
   * it's crucial to have maven and java to have -Dfile.encoding=UTF-8 everywhere! 
   * MAVEN_OPTS=-Dfile.encoding=UTF-8 (or sourceEncoding+resourceEncoding in pom) does it for maven and compile. 
   * But apparently not for the surefire-maven-plugin. Some claims it works with systemPropertiesVariable 
   * but first by explicit putting it on the command line in surefire plugin configuration it worked. 
   * 
   * Damn you, Apple. Get rid of that MacRoman encoding and become 100% UTF-8! 
   * Damn you, Maven plugins for not using parent settings! 
   * 
   */
  String oai_dc = "<oai_dc:dc\n"
      + "	xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n"
      + "	xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
      + "	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
      + "	xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/\n"
      + "	http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
      + "	<dc:title xml:lang=\"en-US\">Advancements in Technology: What&#8217;s in beholding for Human Beings?</dc:title>\n"
      + "	<dc:title xml:lang=\"en-US\">Advancements in Technology: What’s in beholding for Human Beings?</dc:title>\n"
      + "	<dc:creator>S.S. Iyengar; Louisiana State University</dc:creator>\n"
      + "	<dc:subject xml:lang=\"en-US\">Communication Systems</dc:subject>\n"
      + "	<dc:subject xml:lang=\"en-US\">wireless sensor network, virtual reality, information theory, soft computing, data engineering and architecture</dc:subject>\n"
      + "	<dc:subject xml:lang=\"en-US\">Advancements in Technology</dc:subject>\n"
      + "	<dc:description xml:lang=\"en-US\">Science fictions of yesteryears are a reality today. We are now living in a unbelievable tech-age. Technology has enabled human beings to fit visual sensing devices on a fly, make hybrid car capable of running on gas as well as electricity, simulate black hole and eventual evolution of Universe using Large Hadron Collider, and BloomEnergy devising Energy Server of the size of bread loaf; capable of satisfying all energy needs at Fortune 500 incorporations like Google, Staples, Bank of America, eBay, Staples, Walmart and more. The digital revolution has even electronic manufacturers competing neck to neck with each other. What&amp;rsquo;s fastest and latest today, takes no time in becoming history the very next day.</dc:description>\n"
      + "	<dc:publisher xml:lang=\"en-US\">International Journal of Advancements in Technology</dc:publisher>\n"
      + "	<dc:contributor xml:lang=\"en-US\"></dc:contributor>\n"
      + "	<dc:date>2010-10-14</dc:date>\n"
      + "	<dc:type xml:lang=\"en-US\"></dc:type>\n"
      + "	<dc:type xml:lang=\"en-US\"></dc:type>\n"
      + "	<dc:format>application/pdf</dc:format>\n"
      + "	<dc:identifier>http://ijict.org/index.php/ijoat/article/view/advancements-in-technology</dc:identifier>\n"
      + "	<dc:source xml:lang=\"en-US\">International Journal of Advancements in Technology; Vol 1, No 2 (2010): International Journal of Advancements in Technology Vol 1 No 2; 163-165</dc:source>\n"
      + "	<dc:language>en</dc:language>\n"
      + "	<dc:coverage xml:lang=\"en-US\"></dc:coverage>\n"
      + "	<dc:coverage xml:lang=\"en-US\"></dc:coverage>\n"
      + "	<dc:coverage xml:lang=\"en-US\"></dc:coverage>\n"
      + "	<dc:rights>Authors who publish with this journal agree to the following terms:&lt;br /&gt; &lt;ol type=&quot;a&quot;&gt;&lt;br /&gt;&lt;li&gt;Authors retain copyright and grant the journal right of first publication with the work simultaneously licensed under a &lt;a href=&quot;http://creativecommons.org/licenses/by/3.0/&quot; target=&quot;_new&quot;&gt;Creative Commons Attribution License&lt;/a&gt; that allows others to share the work with an acknowledgement of the work's authorship and initial publication in this journal.&lt;/li&gt;&lt;br /&gt;&lt;li&gt;Authors are able to enter into separate, additional contractual arrangements for the non-exclusive distribution of the journal's published version of the work (e.g., post it to an institutional repository or publish it in a book), with an acknowledgement of its initial publication in this journal.&lt;/li&gt;&lt;br /&gt;&lt;li&gt;Authors are permitted and encouraged to post their work online (e.g., in institutional repositories or on their website) prior to and during the submission process, as it can lead to productive exchanges, as well as earlier and greater citation of published work (See &lt;a href=&quot;http://opcit.eprints.org/oacitation-biblio.html&quot; target=&quot;_new&quot;&gt;The Effect of Open Access&lt;/a&gt;).&lt;/li&gt;&lt;/ol&gt;</dc:rights>\n"
      + "</oai_dc:dc>";

  String oai_pmh_marcxml = oai_pmh_records_start + record_add + marcxml + record_end
      + oai_pmh_end_record;

  String oai_pmh_oaidc = oai_pmh_records_start + record_add + oai_dc + record_end + record_delete
      + oai_dc + record_end + oai_pmh_end_record;

  String pz2solr_xsl = "<?xml version=\"1.0\"?>\n"
      + "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" \n"
      + "                xmlns:pz=\"http://www.indexdata.com/pazpar2/1.0\" >\n"
      + "  <xsl:template  match=\"/\">\n" + "    <add>\n"
      + "      <xsl:apply-templates></xsl:apply-templates>\n" + "    </add>\n"
      + "  </xsl:template>\n" + "  <xsl:template match=\"pz:record\">\n" + "    <doc>\n"
      + "      <xsl:apply-templates></xsl:apply-templates>\n" + "    </doc>\n"
      + "  </xsl:template>\n" + "  <xsl:template match=\"pz:metadata\">\n"
      + "    <xsl:if test=\"@type\">\n" + "      <field>\n"
      + "        <xsl:attribute  name=\"name\">\n" + "          <xsl:value-of select=\"@type\"/>\n"
      + "        </xsl:attribute>\n" + "          <xsl:value-of select=\".\"/>\n"
      + "      </field>\n" + "    </xsl:if>\n" + "  </xsl:template>\n" + "</xsl:stylesheet>";

  String oai2marc_xsl = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<xsl:stylesheet\n"
      + "    version=\"1.0\"\n" + "    xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n"
      + "    xmlns:oai=\"http://www.openarchives.org/OAI/2.0/\"\n"
      + "    xmlns:marc=\"http://www.loc.gov/MARC21/slim\"\n"
      + "    xmlns:pz=\"http://www.indexdata.com/pazpar2/1.0\"\n"
      + "    exclude-result-prefixes=\"oai\">\n" + " <xsl:output indent=\"yes\"\n"
      + "        method=\"xml\"\n" + "        version=\"1.0\"\n" + "        encoding=\"UTF-8\"/>\n"
      + "  <xsl:template match=\"//oai:record\">\n" + "      <xsl:variable name=\"oai-id\">\n"
      + "          <xsl:value-of select=\"oai:header/oai:identifier\"/>\n"
      + "      </xsl:variable>\n" + "      <xsl:for-each select=\"oai:metadata/marc:record\">\n"
      + "        <xsl:copy>\n" + "          <xsl:copy-of select=\"@*\"/>\n"
      + "          <xsl:copy-of select=\"*\"/>\n" + "          <pz:metadata type=\"zebra-id\">\n"
      + "           <xsl:value-of select=\"$oai-id\"/>\n" + "          </pz:metadata>\n"
      + "        </xsl:copy>\n" + "      </xsl:for-each>\n" + "  </xsl:template>\n"
      + "  <xsl:template match=\"text()\"/>\n" + "</xsl:stylesheet>";

  String oaidc_pmh_xsl = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<xsl:stylesheet\n"
      + "    version=\"1.0\"\n" + "    xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n"
      + "    xmlns:pz=\"http://www.indexdata.com/pazpar2/1.0\"\n"
      + "    xmlns:oai=\"http://www.openarchives.org/OAI/2.0/\"\n"
      + "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
      + "    xmlns:dcterms=\"http://purl.org/dc/terms/\">\n" + " <xsl:output indent=\"yes\"\n"
      + "        method=\"xml\"\n" + "        version=\"1.0\"\n" + "        encoding=\"UTF-8\"/>\n"
      + "  <xsl:template match=\"//oai:record/oai:metadata/*\">\n" + "    <pz:record>\n"
      + "      <pz:metadata type=\"id\">\n"
      + "        <xsl:value-of select=\"/oai:record/oai:header/oai:identifier\"/>\n"
      + "      </pz:metadata>\n" + "      <xsl:for-each select=\"dc:title\">\n"
      + "        <pz:metadata type=\"title\">\n" + "          <xsl:value-of select=\".\"/>\n"
      + "        </pz:metadata>\n" + "      </xsl:for-each>\n"
      + "      <xsl:for-each select=\"dc:date\">\n" + "        <pz:metadata type=\"date\">\n"
      + "          <xsl:value-of select=\".\"/>\n" + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n" + "      <xsl:for-each select=\"dc:subject\">\n"
      + "        <pz:metadata type=\"subject\">\n" + "          <xsl:value-of select=\".\"/>\n"
      + "        </pz:metadata>\n" + "      </xsl:for-each>\n"
      + "      <xsl:for-each select=\"dc:creator\">\n" + "        <pz:metadata type=\"author\">\n"
      + "          <xsl:value-of select=\".\"/>\n" + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n" + "      <xsl:for-each select=\"dc:description\">\n"
      + "        <pz:metadata type=\"description\">\n" + "          <xsl:value-of select=\".\"/>\n"
      + "        </pz:metadata>\n" + "      </xsl:for-each>\n"
      + "      <xsl:for-each select=\"dc:identifier\">\n"
      + "        <pz:metadata type=\"electronic-url\">\n"
      + "          <xsl:value-of select=\".\"/>\n" + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n" + "      <xsl:for-each select=\"dc:type\">\n"
      + "        <pz:metadata type=\"medium\">\n" + "          <xsl:value-of select=\".\"/>\n"
      + "        </pz:metadata>\n" + "      </xsl:for-each>\n"
      + "      <xsl:for-each select=\"dcterms:bibliographicCitation\">\n"
      + "        <pz:metadata type=\"citation\">\n" + "          <xsl:value-of select=\".\"/>\n"
      + "        </pz:metadata>\n" + "      </xsl:for-each>\n" + "    </pz:record>\n"
      + "  </xsl:template>\n" + "  <xsl:template match=\"text()\"/>\n" + "</xsl:stylesheet>";

  String marc21_xsl = "<xsl:stylesheet\n"
      + "    version=\"1.0\"\n"
      + "    xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n"
      + "    xmlns:pz=\"http://www.indexdata.com/pazpar2/1.0\"\n"
      + "    xmlns:marc=\"http://www.loc.gov/MARC21/slim\">\n"
      + "  \n"
      + "  <xsl:output indent=\"yes\" method=\"xml\" version=\"1.0\" encoding=\"UTF-8\"/>\n"
      + "\n"
      + "<!-- Extract metadata from MARC21/USMARC \n"
      + "      http://www.loc.gov/marc/bibliographic/ecbdhome.html\n"
      + "-->  \n"
      + "  <xsl:template name=\"record-hook\"/>\n"
      + "\n"
      + "  <xsl:template match=\"/\">\n"
      + "    <xsl:apply-templates/>\n"
      + "  </xsl:template>\n"
      + "\n"
      + "  <xsl:template match=\"marc:record\">\n"
      + "    <xsl:variable name=\"title_medium\" select=\"marc:datafield[@tag='245']/marc:subfield[@code='h']\"/>\n"
      + "    <xsl:variable name=\"journal_title\" select=\"marc:datafield[@tag='773']/marc:subfield[@code='t']\"/>\n"
      + "    <xsl:variable name=\"electronic_location_url\" select=\"marc:datafield[@tag='856']/marc:subfield[@code='u']\"/>\n"
      + "    <xsl:variable name=\"fulltext_a\" select=\"marc:datafield[@tag='900']/marc:subfield[@code='a']\"/>\n"
      + "    <xsl:variable name=\"fulltext_b\" select=\"marc:datafield[@tag='900']/marc:subfield[@code='b']\"/>\n"
      + "    <xsl:variable name=\"medium\">\n"
      + "      <xsl:choose>\n"
      + "        <xsl:when test=\"$title_medium\">\n"
      + "          <xsl:value-of select=\"translate($title_medium, ' []/', '')\"/>\n"
      + "        </xsl:when>\n"
      + "        <xsl:when test=\"$fulltext_a\">\n"
      + "          <xsl:text>electronic resource</xsl:text>\n"
      + "        </xsl:when>\n"
      + "        <xsl:when test=\"$fulltext_b\">\n"
      + "          <xsl:text>electronic resource</xsl:text>\n"
      + "        </xsl:when>\n"
      + "        <xsl:when test=\"$journal_title\">\n"
      + "          <xsl:text>article</xsl:text>\n"
      + "        </xsl:when>\n"
      + "        <xsl:otherwise>\n"
      + "          <xsl:text>book</xsl:text>\n"
      + "        </xsl:otherwise>\n"
      + "      </xsl:choose>\n"
      + "    </xsl:variable>\n"
      + "\n"
      + "    <xsl:variable name=\"has_fulltext\">\n"
      + "      <xsl:choose>\n"
      + "        <xsl:when test=\"marc:datafield[@tag='856']/marc:subfield[@code='q']\">\n"
      + "          <xsl:text>yes</xsl:text>\n"
      + "        </xsl:when>\n"
      + "        <xsl:when test=\"marc:datafield[@tag='856']/marc:subfield[@code='i']='TEXT*'\">\n"
      + "          <xsl:text>yes</xsl:text>\n"
      + "        </xsl:when>\n"
      + "        <xsl:otherwise>\n"
      + "          <xsl:text>no</xsl:text>\n"
      + "        </xsl:otherwise>\n"
      + "      </xsl:choose>\n"
      + "    </xsl:variable>\n"
      + "\n"
      + "    <pz:record>\n"
      + "      <xsl:for-each select=\"marc:controlfield[@tag='001']\">\n"
      + "        <pz:metadata type=\"id\">\n"
      + "          <xsl:value-of select=\".\"/>\n"
      + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n"
      + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='010']\">\n"
      + "        <pz:metadata type=\"lccn\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='a']\"/>\n"
      + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n"
      + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='020']\">\n"
      + "        <pz:metadata type=\"isbn\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='a']\"/>\n"
      + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n"
      + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='022']\">\n"
      + "        <pz:metadata type=\"issn\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='a']\"/>\n"
      + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n"
      + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='027']\">\n"
      + "        <pz:metadata type=\"tech-rep-nr\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='a']\"/>\n"
      + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n"
      + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='035']\">\n"
      + "        <pz:metadata type=\"system-control-nr\">\n"
      + "          <xsl:choose>\n"
      + "            <xsl:when test=\"marc:subfield[@code='a']\">\n"
      + "              <xsl:value-of select=\"marc:subfield[@code='a']\"/>\n"
      + "            </xsl:when>\n"
      + "            <xsl:otherwise>\n"
      + "              <xsl:value-of select=\"marc:subfield[@code='b']\"/>\n"
      + "            </xsl:otherwise>\n"
      + "          </xsl:choose>\n"
      + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n"
      + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='100']\">\n"
      + "        <pz:metadata type=\"author\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='a']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"author-title\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='c']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"author-date\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='d']\"/>\n"
      + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n"
      + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='110']\">\n"
      + "        <pz:metadata type=\"corporate-name\">\n"
      + "            <xsl:value-of select=\"marc:subfield[@code='a']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"corporate-location\">\n"
      + "            <xsl:value-of select=\"marc:subfield[@code='c']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"corporate-date\">\n"
      + "            <xsl:value-of select=\"marc:subfield[@code='d']\"/>\n"
      + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n"
      + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='111']\">\n"
      + "        <pz:metadata type=\"meeting-name\">\n"
      + "            <xsl:value-of select=\"marc:subfield[@code='a']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"meeting-location\">\n"
      + "            <xsl:value-of select=\"marc:subfield[@code='c']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"meeting-date\">\n"
      + "            <xsl:value-of select=\"marc:subfield[@code='d']\"/>\n"
      + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n"
      + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='260']\">\n"
      + "        <pz:metadata type=\"date\">\n"
      + "            <xsl:value-of select=\"marc:subfield[@code='c']\"/>\n"
      + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n"
      + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='245']\">\n"
      + "        <pz:metadata type=\"title\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='a']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"title-remainder\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='b']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"title-responsibility\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='c']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"title-dates\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='f']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"title-medium\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='h']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"title-number-section\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='n']\"/>\n"
      + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n"
      + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='250']\">\n"
      + "        <pz:metadata type=\"edition\">\n"
      + "            <xsl:value-of select=\"marc:subfield[@code='a']\"/>\n"
      + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n"
      + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='260']\">\n"
      + "        <pz:metadata type=\"publication-place\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='a']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"publication-name\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='b']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"publication-date\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='c']\"/>\n"
      + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n"
      + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='300']\">\n"
      + "        <pz:metadata type=\"physical-extent\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='a']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"physical-format\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='b']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"physical-dimensions\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='c']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"physical-accomp\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='e']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"physical-unittype\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='f']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"physical-unitsize\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='g']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"physical-specified\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='3']\"/>\n"
      + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n"
      + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='440']\">\n"
      + "        <pz:metadata type=\"series-title\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='a']\"/>\n"
      + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n"
      + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag = '500' or @tag = '505' or\n"
      + "                @tag = '518' or @tag = '520' or @tag = '522']\">\n"
      + "        <pz:metadata type=\"description\">\n"
      + "            <xsl:value-of select=\"*/text()\"/>\n"
      + "        </pz:metadata>\n"
      + "      </xsl:for-each>\n"
      + "      \n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='600' or @tag='610' or @tag='611' or @tag='630' or @tag='648' or @tag='650' or @tag='651' or @tag='653' or @tag='654' or @tag='655' or @tag='656' or @tag='657' or @tag='658' or @tag='662' or @tag='69X']\">\n"
      + "        <pz:metadata type=\"subject\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='a']\"/>\n"
      + "        </pz:metadata>\n" + "        <pz:metadata type=\"subject-long\">\n"
      + "          <xsl:for-each select=\"marc:subfield\">\n"
      + "            <xsl:if test=\"position() > 1\">\n"
      + "              <xsl:text>, </xsl:text>\n" + "            </xsl:if>\n"
      + "            <xsl:value-of select=\".\"/>\n" + "          </xsl:for-each>\n"
      + "        </pz:metadata>\n" + "      </xsl:for-each>\n" + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='856']\">\n"
      + "        <pz:metadata type=\"electronic-url\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='u']\"/>\n"
      + "        </pz:metadata>\n" + "        <pz:metadata type=\"electronic-text\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='y' or @code='3']\"/>\n"
      + "        </pz:metadata>\n" + "        <pz:metadata type=\"electronic-note\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='z']\"/>\n"
      + "        </pz:metadata>\n"
      + "        <pz:metadata type=\"electronic-format-instruction\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='i']\"/>\n"
      + "        </pz:metadata>\n" + "        <pz:metadata type=\"electronic-format-type\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='q']\"/>\n"
      + "        </pz:metadata>\n" + "      </xsl:for-each>\n" + "\n"
      + "      <pz:metadata type=\"has-fulltext\">\n"
      + "        <xsl:value-of select=\"$has_fulltext\"/> \n" + "      </pz:metadata>\n" + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='773']\">\n"
      + "        <pz:metadata type=\"citation\">\n" + "              <xsl:for-each select=\"*\">\n"
      + "                <xsl:value-of select=\"normalize-space(.)\"/>\n"
      + "                <xsl:text> </xsl:text>\n" + "          </xsl:for-each>\n"
      + "        </pz:metadata>\n" + "        <xsl:if test=\"marc:subfield[@code='t']\">\n"
      + "          <pz:metadata type=\"journal-title\">\n"
      + "                <xsl:value-of select=\"marc:subfield[@code='t']\"/>\n"
      + "          </pz:metadata>          \n" + "        </xsl:if>\n"
      + "        <xsl:if test=\"marc:subfield[@code='g']\">\n"
      + "          <pz:metadata type=\"journal-subpart\">\n"
      + "                <xsl:value-of select=\"marc:subfield[@code='g']\"/>\n"
      + "          </pz:metadata>          \n" + "        </xsl:if>\n" + "      </xsl:for-each>\n"
      + "\n" + "      <xsl:for-each select=\"marc:datafield[@tag='852']\">\n"
      + "        <xsl:if test=\"marc:subfield[@code='y']\">\n"
      + "          <pz:metadata type=\"publicnote\">\n"
      + "            <xsl:value-of select=\"marc:subfield[@code='y']\"/>\n"
      + "          </pz:metadata>\n" + "        </xsl:if>\n"
      + "        <xsl:if test=\"marc:subfield[@code='h']\">\n"
      + "          <pz:metadata type=\"callnumber\">\n"
      + "            <xsl:value-of select=\"marc:subfield[@code='h']\"/>\n"
      + "          </pz:metadata>\n" + "        </xsl:if>\n" + "      </xsl:for-each>\n" + "\n"
      + "      <pz:metadata type=\"medium\">\n" + "        <xsl:value-of select=\"$medium\"/>\n"
      + "      </pz:metadata>\n" + "      \n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='900']/marc:subfield[@code='a']\">\n"
      + "        <pz:metadata type=\"fulltext\">\n" + "          <xsl:value-of select=\".\"/>\n"
      + "        </pz:metadata>\n" + "      </xsl:for-each>\n" + "\n"
      + "      <!-- <xsl:if test=\"$fulltext_a\">\n" + "        <pz:metadata type=\"fulltext\">\n"
      + "          <xsl:value-of select=\"$fulltext_a\"/>\n" + "        </pz:metadata>\n"
      + "      </xsl:if> -->\n" + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='900']/marc:subfield[@code='b']\">\n"
      + "        <pz:metadata type=\"fulltext\">\n" + "          <xsl:value-of select=\".\"/>\n"
      + "        </pz:metadata>\n" + "      </xsl:for-each>\n" + "\n"
      + "      <!-- <xsl:if test=\"$fulltext_b\">\n" + "        <pz:metadata type=\"fulltext\">\n"
      + "          <xsl:value-of select=\"$fulltext_b\"/>\n" + "        </pz:metadata>\n"
      + "      </xsl:if> -->\n" + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='907' or @tag='901']\">\n"
      + "        <pz:metadata type=\"iii-id\">\n"
      + "          <xsl:value-of select=\"marc:subfield[@code='a']\"/>\n"
      + "        </pz:metadata>\n" + "      </xsl:for-each>\n" + "\n"
      + "      <xsl:for-each select=\"marc:datafield[@tag='926']\">\n"
      + "        <pz:metadata type=\"holding\">\n"
      + "          <xsl:for-each select=\"marc:subfield\">\n"
      + "            <xsl:if test=\"position() > 1\">\n" + "              <xsl:text> </xsl:text>\n"
      + "            </xsl:if>\n" + "            <xsl:value-of select=\".\"/>\n"
      + "          </xsl:for-each>\n" + "        </pz:metadata>\n" + "      </xsl:for-each>\n"
      + "\n" + "      <xsl:for-each select=\"marc:datafield[@tag='948']\">\n"
      + "        <pz:metadata type=\"holding\">\n"
      + "          <xsl:for-each select=\"marc:subfield\">\n"
      + "            <xsl:if test=\"position() > 1\">\n" + "              <xsl:text> </xsl:text>\n"
      + "            </xsl:if>\n" + "            <xsl:value-of select=\".\"/>\n"
      + "          </xsl:for-each>\n" + "        </pz:metadata>\n" + "      </xsl:for-each>\n"
      + "\n" + "      <xsl:for-each select=\"marc:datafield[@tag='991']\">\n"
      + "        <pz:metadata type=\"holding\">\n"
      + "          <xsl:for-each select=\"marc:subfield\">\n"
      + "            <xsl:if test=\"position() > 1\">\n" + "              <xsl:text> </xsl:text>\n"
      + "            </xsl:if>\n" + "            <xsl:value-of select=\".\"/>\n"
      + "          </xsl:for-each>\n" + "        </pz:metadata>\n" + "      </xsl:for-each>\n"
      + "\n" + "      <!-- passthrough id data -->\n"
      + "      <xsl:for-each select=\"pz:metadata\">\n" + "          <xsl:copy-of select=\".\"/>\n"
      + "      </xsl:for-each>\n" + "\n"
      + "      <!-- other stylesheets importing this might want to define this -->\n"
      + "      <xsl:call-template name=\"record-hook\"/>\n" + "\n" + "    </pz:record>    \n"
      + "  </xsl:template>\n" + "  \n" + "  <xsl:template match=\"text()\"/>\n" + "\n"
      + "</xsl:stylesheet>";

  String DC2marcslim_xsl 
  	      = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
  		"<xsl:stylesheet version=\"1.0\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns=\"http://www.loc.gov/MARC21/slim\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" exclude-result-prefixes=\"dc\">\n" + 
  		"	<xsl:output method=\"xml\" indent=\"yes\"/>\n" + 
  		"	\n" + 
  		"	<xsl:template match=\"/\">\n" + 
  		"		<record xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd\" >\n" + 
  		"			<xsl:element name=\"leader\">\n" + 
  		"				<xsl:variable name=\"type\" select=\"dc:type\"/>\n" + 
  		"				<xsl:variable name=\"leader06\">\n" + 
  		"					<xsl:choose>\n" + 
  		"						<xsl:when test=\"$type='collection'\">p</xsl:when>\n" + 
  		"						<xsl:when test=\"$type='dataset'\">m</xsl:when>\n" + 
  		"						<xsl:when test=\"$type='event'\">r</xsl:when>\n" + 
  		"						<xsl:when test=\"$type='image'\">k</xsl:when>\n" + 
  		"						<xsl:when test=\"$type='interactive resource'\">m</xsl:when>\n" + 
  		"						<xsl:when test=\"$type='service'\">m</xsl:when>\n" + 
  		"						<xsl:when test=\"$type='software'\">m</xsl:when>\n" + 
  		"						<xsl:when test=\"$type='sound'\">i</xsl:when>\n" + 
  		"						<xsl:when test=\"$type='text'\">a</xsl:when>\n" + 
  		"						<xsl:otherwise>a</xsl:otherwise>\n" + 
  		"					</xsl:choose>\n" + 
  		"				</xsl:variable>\n" + 
  		"				<xsl:variable name=\"leader07\">\n" + 
  		"					<xsl:choose>\n" + 
  		"						<xsl:when test=\"$type='collection'\">c</xsl:when>\n" + 
  		"						<xsl:otherwise>m</xsl:otherwise>\n" + 
  		"					</xsl:choose>\n" + 
  		"				</xsl:variable>\n" + 
  		"				<xsl:value-of select=\"concat('      ',$leader06,$leader07,'         3u     ')\"/>\n" + 
  		"			</xsl:element>\n" + 
  		"\n" + 
  		"			<datafield tag=\"042\" ind1=\" \" ind2=\" \">\n" + 
  		"				<subfield code=\"a\">dc</subfield>\n" + 
  		"			</datafield>\n" + 
  		"\n" + 
  		"			<xsl:for-each select=\"//dc:contributer\">\n" + 
  		"				<datafield tag=\"720\" ind1=\"0\" ind2=\"0\">\n" + 
  		"					<subfield code=\"a\">\n" + 
  		"						<xsl:value-of select=\".\"/>\n" + 
  		"					</subfield>\n" + 
  		"					<subfield code=\"e\">collaborator</subfield>\n" + 
  		"				</datafield>\n" + 
  		"			</xsl:for-each>\n" + 
  		"\n" + 
  		"			<xsl:for-each select=\"//dc:coverage\">\n" + 
  		"				<datafield tag=\"500\" ind1=\" \" ind2=\" \">\n" + 
  		"					<subfield code=\"a\">\n" + 
  		"						<xsl:value-of select=\".\"/>\n" + 
  		"					</subfield>\n" + 
  		"				</datafield>\n" + 
  		"			</xsl:for-each>\n" + 
  		"\n" + 
  		"			<xsl:for-each select=\"//dc:creator\">\n" + 
  		"				<datafield tag=\"720\" ind1=\" \" ind2=\" \">\n" + 
  		"					<subfield code=\"a\">\n" + 
  		"						<xsl:value-of select=\".\"/>\n" + 
  		"					</subfield>\n" + 
  		"					<subfield code=\"e\">author</subfield>\n" + 
  		"				</datafield>\n" + 
  		"			</xsl:for-each>\n" + 
  		"\n" + 
  		"			<xsl:for-each select=\"//dc:date\">\n" + 
  		"				<datafield tag=\"260\" ind1=\" \" ind2=\" \">\n" + 
  		"					<subfield code=\"c\">\n" + 
  		"						<xsl:value-of select=\".\"/>\n" + 
  		"					</subfield>\n" + 
  		"				</datafield>\n" + 
  		"			</xsl:for-each>	\n" + 
  		"\n" + 
  		"			<xsl:for-each select=\"//dc:description\">\n" + 
  		"				<datafield tag=\"520\" ind1=\" \" ind2=\" \">\n" + 
  		"					<subfield code=\"a\">\n" + 
  		"						<xsl:value-of select=\".\"/>\n" + 
  		"					</subfield>\n" + 
  		"				</datafield>\n" + 
  		"			</xsl:for-each>\n" + 
  		"			\n" + 
  		"			<xsl:for-each select=\"//dc:format\">\n" + 
  		"				<datafield tag=\"856\" ind1=\" \" ind2=\" \">\n" + 
  		"					<subfield code=\"q\">\n" + 
  		"						<xsl:value-of select=\".\"/>\n" + 
  		"					</subfield>\n" + 
  		"				</datafield>\n" + 
  		"			</xsl:for-each>\n" + 
  		"\n" + 
  		"			<xsl:for-each select=\"//dc:identifier\">\n" + 
  		"				<datafield tag=\"024\" ind1=\"8\" ind2=\" \">\n" + 
  		"					<subfield code=\"a\">\n" + 
  		"						<xsl:value-of select=\".\"/>\n" + 
  		"					</subfield>\n" + 
  		"				</datafield>\n" + 
  		"			</xsl:for-each>\n" + 
  		"\n" + 
  		"			<xsl:for-each select=\"//dc:language\">\n" + 
  		"				<datafield tag=\"546\" ind1=\" \" ind2=\" \">\n" + 
  		"					<subfield code=\"a\">\n" + 
  		"						<xsl:value-of select=\".\"/>\n" + 
  		"					</subfield>\n" + 
  		"				</datafield>\n" + 
  		"			</xsl:for-each>\n" + 
  		"			\n" + 
  		"			<xsl:for-each select=\"//dc:publisher\">\n" + 
  		"				<datafield tag=\"260\" ind1=\" \" ind2=\" \">\n" + 
  		"					<subfield code=\"b\">\n" + 
  		"						<xsl:value-of select=\".\"/>\n" + 
  		"					</subfield>\n" + 
  		"				</datafield>\n" + 
  		"			</xsl:for-each>\n" + 
  		"\n" + 
  		"			<xsl:for-each select=\"//dc:relation\">\n" + 
  		"				<datafield tag=\"787\" ind1=\"0\" ind2=\" \">\n" + 
  		"					<subfield code=\"n\">\n" + 
  		"						<xsl:value-of select=\".\"/>\n" + 
  		"					</subfield>\n" + 
  		"				</datafield>\n" + 
  		"			</xsl:for-each>\n" + 
  		"\n" + 
  		"			<xsl:for-each select=\"//dc:rights\">\n" + 
  		"				<datafield tag=\"540\" ind1=\" \" ind2=\" \">\n" + 
  		"					<subfield code=\"a\">\n" + 
  		"						<xsl:value-of select=\".\"/>\n" + 
  		"					</subfield>\n" + 
  		"				</datafield>\n" + 
  		"			</xsl:for-each>\n" + 
  		"\n" + 
  		"			<xsl:for-each select=\"//dc:source\">\n" + 
  		"				<datafield tag=\"786\" ind1=\"0\" ind2=\" \">\n" + 
  		"					<subfield code=\"n\">\n" + 
  		"						<xsl:value-of select=\".\"/>\n" + 
  		"					</subfield>\n" + 
  		"				</datafield>\n" + 
  		"			</xsl:for-each>\n" + 
  		"\n" + 
  		"			<xsl:for-each select=\"//dc:subject\">\n" + 
  		"				<datafield tag=\"653\" ind1=\" \" ind2=\" \">\n" + 
  		"					<subfield code=\"a\">\n" + 
  		"						<xsl:value-of select=\".\"/>\n" + 
  		"					</subfield>\n" + 
  		"				</datafield>\n" + 
  		"			</xsl:for-each>\n" + 
  		"																							\n" + 
  		"			<xsl:for-each select=\"//dc:title[1]\">\n" + 
  		"				<datafield tag=\"245\" ind1=\"0\" ind2=\"0\">\n" + 
  		"					<subfield code=\"a\">\n" + 
  		"						<xsl:value-of select=\".\"/>\n" + 
  		"					</subfield>\n" + 
  		"				</datafield>\n" + 
  		"			</xsl:for-each>\n" + 
  		"\n" + 
  		"			<xsl:for-each select=\"//dc:title[position()>1]\">\n" + 
  		"				<datafield tag=\"246\" ind1=\"3\" ind2=\"3\">\n" + 
  		"					<subfield code=\"a\">\n" + 
  		"						<xsl:value-of select=\".\"/>\n" + 
  		"					</subfield>\n" + 
  		"				</datafield>\n" + 
  		"			</xsl:for-each>\n" + 
  		"\n" + 
  		"			<xsl:for-each select=\"//dc:type\">\n" + 
  		"				<datafield tag=\"655\" ind1=\"7\" ind2=\" \">\n" + 
  		"					<subfield code=\"a\">\n" + 
  		"						<xsl:value-of select=\".\"/>\n" + 
  		"					</subfield>\n" + 
  		"					<subfield code=\"2\">local</subfield>\n" + 
  		"				</datafield>\n" + 
  		"			</xsl:for-each>\n" + 
  		"		</record>\n" + 
  		"	</xsl:template>\n" + 
  		"</xsl:stylesheet>";
  
  	String GPDC_2_marc_xsl =
  	    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
  	    "<xsl:stylesheet version=\"1.0\" \n" + 
  	    "		xmlns:dc=\"http://purl.org/dc/elements/1.1/\" \n" + 
  	    "		xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" + 
  	    "		xmlns=\"http://www.loc.gov/MARC21/slim\" \n" + 
  	    "		xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" exclude-result-prefixes=\"dc\" \n" + 
  	    "		xmlns:pgterms=\"http://www.gutenberg.org/rdfterms/\">\n" + 
  	    "    <xsl:output method=\"xml\" indent=\"yes\"/>\n" + 
  	    "    <xsl:strip-space elements=\"pgterms:file\"/>\n" + 
  	    "    <xsl:template match=\"/\">\n" + 
  	    "      <collection>\n" + 
  	    "	<xsl:apply-templates />\n" + 
  	    "      </collection>\n" + 
  	    "    </xsl:template>\n" + 
  	    "    <xsl:template match=\"rdf:Description\">\n" + 
  	    "      <xsl:comment>\n" + 
  	    "	<xsl:value-of select=\".\" />\n" + 
  	    "      </xsl:comment>\n" + 
  	    "    </xsl:template>\n" + 
  	    "    <xsl:template match=\"pgterms:etext\">\n" + 
  	    "        <record>\n" + 
  	    "            <xsl:element name=\"leader\">\n" + 
  	    "                <xsl:variable name=\"type\" select=\"dc:type\"/>\n" + 
  	    "                <xsl:variable name=\"leader06\">\n" + 
  	    "                    <xsl:choose>\n" + 
  	    "                        <xsl:when test=\"$type='collection'\">p</xsl:when>\n" + 
  	    "                        <xsl:when test=\"$type='dataset'\">m</xsl:when>\n" + 
  	    "                        <xsl:when test=\"$type='event'\">r</xsl:when>\n" + 
  	    "                        <xsl:when test=\"$type='image'\">k</xsl:when>\n" + 
  	    "                        <xsl:when test=\"$type='interactive resource'\">m</xsl:when>\n" + 
  	    "                        <xsl:when test=\"$type='service'\">m</xsl:when>\n" + 
  	    "                        <xsl:when test=\"$type='software'\">m</xsl:when>\n" + 
  	    "                        <xsl:when test=\"$type='sound'\">i</xsl:when>\n" + 
  	    "                        <xsl:when test=\"$type='text'\">a</xsl:when>\n" + 
  	    "                        <xsl:otherwise>a</xsl:otherwise>\n" + 
  	    "                    </xsl:choose>\n" + 
  	    "                </xsl:variable>\n" + 
  	    "                <xsl:variable name=\"leader07\">\n" + 
  	    "                    <xsl:choose>\n" + 
  	    "                        <xsl:when test=\"$type='collection'\">c</xsl:when>\n" + 
  	    "                        <xsl:otherwise>m</xsl:otherwise>\n" + 
  	    "                    </xsl:choose>\n" + 
  	    "                </xsl:variable>\n" + 
  	    "                <xsl:value-of select=\"concat('      ',$leader06,$leader07,'         3u     ')\"/>\n" + 
  	    "            </xsl:element>\n" + 
  	    "	    <controlfield tag=\"001\">\n" + 
  	    "	      <xsl:value-of select=\"substring(@rdf:ID, 6)\"/>\n" + 
  	    "	    </controlfield>\n" + 
  	    "	    <datafield tag=\"856\" ind1=\"0\" ind2=\"0\">\n" + 
  	    "	      <subfield code=\"u\">http://www.gutenberg.org/ebooks/<xsl:value-of select=\"substring(@rdf:ID, 6)\"/></subfield>\n" + 
  	    "	    </datafield>\n" + 
  	    "\n" + 
  	    "            <xsl:for-each select=\"dc:contributer\">\n" + 
  	    "                <datafield tag=\"720\" ind1=\"0\" ind2=\"0\">\n" + 
  	    "                    <subfield code=\"a\">\n" + 
  	    "                        <xsl:value-of select=\".\"/>\n" + 
  	    "                    </subfield>\n" + 
  	    "                    <subfield code=\"e\">collaborator</subfield>\n" + 
  	    "                </datafield>\n" + 
  	    "            </xsl:for-each>\n" + 
  	    "\n" + 
  	    "            <xsl:for-each select=\"dc:coverage\">\n" + 
  	    "                <datafield tag=\"500\" ind1=\" \" ind2=\" \">\n" + 
  	    "                    <subfield code=\"a\">\n" + 
  	    "                        <xsl:value-of select=\".\"/>\n" + 
  	    "                    </subfield>\n" + 
  	    "                </datafield>\n" + 
  	    "            </xsl:for-each>\n" + 
  	    "\n" + 
  	    "            <xsl:for-each select=\"dc:creator\">\n" + 
  	    "                <datafield tag=\"100\" ind1=\" \" ind2=\" \">\n" + 
  	    "                    <subfield code=\"a\">\n" + 
  	    "                        <xsl:value-of select=\".\"/>\n" + 
  	    "                    </subfield>\n" + 
  	    "<!--\n" + 
  	    "                    <subfield code=\"e\">author</subfield>\n" + 
  	    "-->\n" + 
  	    "                </datafield>\n" + 
  	    "            </xsl:for-each>\n" + 
  	    "\n" + 
  	    "            <xsl:for-each select=\"dc:date\">\n" + 
  	    "                <datafield tag=\"260\" ind1=\" \" ind2=\" \">\n" + 
  	    "                    <subfield code=\"c\">\n" + 
  	    "                        <xsl:value-of select=\".\"/>\n" + 
  	    "                    </subfield>\n" + 
  	    "                </datafield>\n" + 
  	    "            </xsl:for-each>    \n" + 
  	    "\n" + 
  	    "            <xsl:for-each select=\"dc:description\">\n" + 
  	    "                <datafield tag=\"520\" ind1=\" \" ind2=\" \">\n" + 
  	    "                    <subfield code=\"a\">\n" + 
  	    "                        <xsl:value-of select=\".\"/>\n" + 
  	    "                    </subfield>\n" + 
  	    "                </datafield>\n" + 
  	    "            </xsl:for-each>\n" + 
  	    "\n" + 
  	    "            <xsl:for-each select=\"dc:identifier\">\n" + 
  	    "                <datafield tag=\"856\" ind1=\"4\" ind2=\"0\">\n" + 
  	    "                    <subfield code=\"u\">\n" + 
  	    "                        <xsl:value-of select=\".\"/>\n" + 
  	    "                    </subfield>\n" + 
  	    "		    <xsl:if test=\"dc:format\">\n" + 
  	    "		      <subfield code=\"q\">\n" + 
  	    "		        <xsl:value-of select=\"dc:format\"/>\n" + 
  	    "		      </subfield>\n" + 
  	    "		    </xsl:if>\n" + 
  	    "                </datafield>\n" + 
  	    "            </xsl:for-each>\n" + 
  	    "\n" + 
  	    "            <xsl:for-each select=\"dc:language\">\n" + 
  	    "                <datafield tag=\"546\" ind1=\" \" ind2=\" \">\n" + 
  	    "                    <subfield code=\"a\">\n" + 
  	    "                        <xsl:value-of select=\".\"/>\n" + 
  	    "                    </subfield>\n" + 
  	    "                </datafield>\n" + 
  	    "            </xsl:for-each>\n" + 
  	    "\n" + 
  	    "            <xsl:for-each select=\"dc:publisher\">\n" + 
  	    "                <datafield tag=\"260\" ind1=\" \" ind2=\" \">\n" + 
  	    "                    <subfield code=\"b\">\n" + 
  	    "                        <xsl:value-of select=\".\"/>\n" + 
  	    "                    </subfield>\n" + 
  	    "                </datafield>\n" + 
  	    "            </xsl:for-each>\n" + 
  	    "\n" + 
  	    "            <xsl:for-each select=\"dc:relation\">\n" + 
  	    "                <datafield tag=\"787\" ind1=\"0\" ind2=\" \">\n" + 
  	    "                    <subfield code=\"n\">\n" + 
  	    "                        <xsl:value-of select=\".\"/>\n" + 
  	    "                    </subfield>\n" + 
  	    "                </datafield>\n" + 
  	    "            </xsl:for-each>\n" + 
  	    "\n" + 
  	    "            <xsl:for-each select=\"dc:rights\">\n" + 
  	    "                <datafield tag=\"540\" ind1=\" \" ind2=\" \">\n" + 
  	    "                    <subfield code=\"a\">\n" + 
  	    "                        <xsl:value-of select=\".\"/>\n" + 
  	    "                    </subfield>\n" + 
  	    "                </datafield>\n" + 
  	    "            </xsl:for-each>\n" + 
  	    "\n" + 
  	    "            <xsl:for-each select=\"dc:source\">\n" + 
  	    "                <datafield tag=\"786\" ind1=\"0\" ind2=\" \">\n" + 
  	    "                    <subfield code=\"n\">\n" + 
  	    "                        <xsl:value-of select=\".\"/>\n" + 
  	    "                    </subfield>\n" + 
  	    "                </datafield>\n" + 
  	    "            </xsl:for-each>\n" + 
  	    "\n" + 
  	    "            <datafield tag=\"650\" ind1=\" \" ind2=\" \">\n" + 
  	    "	      <xsl:for-each select=\"dc:subject\">\n" + 
  	    "<!--		    <xsl:for-each select=\"tokenize(.,';|- -')\"> -->\n" + 
  	    "		      <subfield code=\"a\"><xsl:value-of select=\"normalize-space(.)\" /></subfield>\n" + 
  	    "<!--		    </xsl:for-each> -->\n" + 
  	    "              </xsl:for-each>\n" + 
  	    "	    </datafield>\n" + 
  	    "\n" + 
  	    "            <xsl:for-each select=\"dc:title[1]\">\n" + 
  	    "                <datafield tag=\"245\" ind1=\"0\" ind2=\"0\">\n" + 
  	    "                    <subfield code=\"a\">\n" + 
  	    "                        <xsl:value-of select=\".\"/>\n" + 
  	    "                    </subfield>\n" + 
  	    "                </datafield>\n" + 
  	    "            </xsl:for-each>\n" + 
  	    "\n" + 
  	    "            <xsl:for-each select=\"dc:title[position()>1]\">\n" + 
  	    "                <datafield tag=\"246\" ind1=\"3\" ind2=\"3\"><subfield code=\"a\"><xsl:value-of select=\".\"/></subfield></datafield>\n" + 
  	    "            </xsl:for-each>\n" + 
  	    "\n" + 
  	    "            <xsl:for-each select=\"dc:type\">\n" + 
  	    "                <datafield tag=\"655\" ind1=\"7\" ind2=\" \">\n" + 
  	    "                    <subfield code=\"a\">\n" + 
  	    "                        <xsl:value-of select=\".\"/>\n" + 
  	    "                    </subfield>\n" + 
  	    "                    <subfield code=\"2\">local</subfield>\n" + 
  	    "                </datafield>\n" + 
  	    "            </xsl:for-each>\n" + 
  	    "        </record>\n" + 
  	    "    </xsl:template>\n" + 
  	    "  <!-- Ignore other elements by mapping into empty DOM XML trees -->\n" + 
  	    "<!--\n" + 
  	    "  <xsl:template match=\"/*\"/>\n" + 
  	    "-->\n" + 
  	    "  <xsl:template match=\"pgterms:file\" />\n" + 
  	    "\n" + 
  	    "</xsl:stylesheet>\n";
  
}
