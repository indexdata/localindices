package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.SolrStorageEntity;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.XmlBulkResource;
import com.indexdata.masterkey.localindices.harvest.storage.BulkSolrRecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.SolrRecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.StatusNotImplemented;
import com.indexdata.masterkey.localindices.harvest.storage.StorageStatus;

public class TestBulkRecordHarvestJob extends JobTester {

  private static final int NO_RECORDS = 1002;
  // String resourceMarc0 = "http://lui-dev.indexdata.com/ag/demo-part-00.mrc";
  private String resourceMarc0 = "http://lui-dev.indexdata.com/loc/loc-small.0000000";
  private String resourceMarc1 = "http://lui-dev.indexdata.com/loc/loc-small.0000001";
  private String resourceMarc2 = "http://lui-dev.indexdata.com/loc/loc-small.0000002";
  // private String resourceMarcXml0 = "http://lui-dev.indexdata.com/loc/loc-small.0000000.xml";
  // String resourceTurboMarc0 = "http://lui-dev.indexdata.com/loc/loc-small.0000000.txml";
  String resourceMarcUTF8 = "http://lui-dev.indexdata.com/oaister/oais.000000.mrc";
  String resourceMarcUTF8gzipped = "http://lui-dev.indexdata.com/oaister/oais.000000.mrc.gz";

  String resourceOaiPmh = "http://lui-dev.indexdata.com/oaipmh/Harvester_Full_1.xml";

  // String resourceLoCMarc8gz = "http://lui-dev.indexdata.com/loc/part01.dat.gz";
  //private String resourceOAIster = "http://lui-dev.indexdata.com/marcdata/meta/oaister/harvester-index.html";
  private String resourceMarcGZ = "http://lui-dev.indexdata.com/ag/demo-part-00.mrc.gz";
  private String resourceMarcZIP = "http://lui-dev.indexdata.com/ag/demo-part-00.mrc.zip";
  private String resourceMarcZIPMulti = "http://lui-dev.indexdata.com/zip/marc-multi.zip";
  private String resourceMarcXmlZIPMulti = "http://lui-dev.indexdata.com/zip/koha-marcxml-multi.zip";
  // String resourceMarcXmlZIPMulti = "http://lui-dev.indexdata.com/marcdata/archive.org/b3kat/b3kat_export_2011_teil21-25_new.zip";
  // private String resourceTurboMarcZIPMulti = "http://lui-dev.indexdata.com/zip/koha-turbomarc-multi.zip";
  private String solrUrl = "http://localhost:8585/solr/";
  private String solrBadUrl = "http://localhost:8686/solrbad/";
  //SolrServerFactory factory = new EmbeddedSolrServerFactory(solrUrl);
  //SolrServer solrServer = factory.create();
  @SuppressWarnings("unused")
  private final static Logger logger = Logger.getLogger("com.indexdata.masterkey");

  private XmlBulkResource createResource(String url, String expectedSchema, String outputSchema,
      String splitAt, String size, boolean overwrite) throws IOException {
    XmlBulkResource resource = new XmlBulkResource(url);
    resource.setName(url + " " + (expectedSchema != null ? expectedSchema + " " : "") + splitAt
	+ " " + size);
    resource.setSplitAt(splitAt);
    resource.setSplitSize(size);
    resource.setExpectedSchema(expectedSchema);
    resource.setOutputSchema(outputSchema);
    resource.setEnabled(true);
    resource.setId(1l);
    resource.setCurrentStatus("NEW");
    resource.setOverwrite(overwrite);
    return resource;
  }

  private Harvestable createResource(String url, String expectedSchema, String outputSchema,
      int splitAt, int size, boolean overwrite) throws IOException {
    return createResource(url, expectedSchema, outputSchema, String.valueOf(splitAt),
	String.valueOf(size), overwrite);
  }

  private RecordHarvestJob doHarvestJob(RecordStorage recordStorage, Harvestable resource)
      throws IOException {
    AbstractRecordHarvestJob job = new BulkRecordHarvestJob((XmlBulkResource) resource, null, new DummyJobNotifications());
    job.setLogger(new ConsoleStorageJobLogger(job.getClass(), resource));
    job.setStorage(recordStorage);
    job.run();
    return job;
  }

  private Transformation createMarc21Transformation(boolean inParallel) throws IOException {
    String[] resourceSteps = {
	"class:com.indexdata.masterkey.localindices.harvest.messaging.XmlLoggerRouter",
	"resources/marc21.xsl",
	"class:com.indexdata.masterkey.localindices.harvest.messaging.XmlLoggerRouter",
	"resources/pz2-create-id.xsl",
	"class:com.indexdata.masterkey.localindices.harvest.messaging.XmlLoggerRouter" };
    return createTransformationFromResources(resourceSteps, inParallel);
  }

  private Transformation createTurboMarcTransformation(boolean inParallel) throws IOException {
    String[] resourceSteps = { "resources/tmarc.xsl", "resources/pz2-create-id.xsl" };
    return createTransformationFromResources(resourceSteps, inParallel);
  }

  private RecordStorage initializeStorage(boolean clear, Harvestable resource,
      RecordStorage recordStorage) throws IOException, StatusNotImplemented {

    // To be sure we have the committed records available
    if (recordStorage instanceof SolrRecordStorage)
      ((SolrRecordStorage) recordStorage).setWaitSearcher(true);

    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));
    if (clear) {
      purgeStorage(recordStorage);
    }
    return recordStorage;
  }

  private RecordStorage createStorage(boolean clear, Harvestable resource) throws IOException,
      StatusNotImplemented {
    SolrStorageEntity storageEntity = new SolrStorageEntity(); 
    storageEntity.setId(resource.getId());
    storageEntity.setName(solrUrl);
    storageEntity.setUrl(solrUrl);
    resource.setStorage(storageEntity);
    return initializeStorage(clear, resource, new BulkSolrRecordStorage(resource));
  }

  private class StorageCreator {
    protected RecordStorage storage;

    StorageCreator() throws IOException, StatusNotImplemented {
    }

    StorageCreator(RecordStorage storage) throws IOException, StatusNotImplemented {
      this.storage = storage;
    }

    RecordStorage createStorage(Harvestable resource) {
      storage = new BulkSolrRecordStorage(resource);
      return storage;
    }

    RecordStorage createStorage(boolean clear, Harvestable resource) throws IOException,
	StatusNotImplemented {
      if (storage != null)
	return storage;
      createStorage(resource);
      return initializeStorage(clear, resource, storage);
    }
  }

  @SuppressWarnings("unused")
  private class CustomUrlStorageCreator extends StorageCreator {
    String url;

    CustomUrlStorageCreator(String storageUrl) throws IOException, StatusNotImplemented {
      url = storageUrl;
    }

    @Override
    RecordStorage createStorage(Harvestable resource) {
      return new BulkSolrRecordStorage(resource);
    }
  }

  @SuppressWarnings("unused")
  private class CustomStorageCreator extends StorageCreator {
    CustomStorageCreator(RecordStorage custom) throws IOException, StatusNotImplemented {
      super(custom);
    }
  }

  private void testMarc21SplitByNumber(boolean inParallel, int number, boolean clear,
      boolean overwrite, long expected_total) throws IOException, StatusNotImplemented {
    Harvestable resource = createResource(resourceMarc0, "application/marc;charset=MARC8",
	null, 0, number, overwrite);
    resource.setId(1l);
    resource.setTransformation(createMarc21Transformation(inParallel));

    RecordStorage recordStorage = createStorage(clear, resource);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    assertTrue("Job not finished: " + job.getStatus(), job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), NO_RECORDS, 0, NO_RECORDS);
  }

  private void purgeStorage(RecordStorage recordStorage) throws IOException, StatusNotImplemented {
    recordStorage.begin();
    recordStorage.purge(true);
    recordStorage.commit();
    StorageStatus storageStatus = recordStorage.getStatus();
    long total = storageStatus.getTotalRecords();
    assertTrue("Total records != 0: " + total, total == 0);
  }

  private void testCleanMarc21SplitByNumber(boolean inParallel, int number) throws IOException,
      StatusNotImplemented {
    testMarc21SplitByNumber(inParallel, number, true, true, NO_RECORDS);
  }

  public void testCleanMarc21NoSplitSerial() throws IOException, StatusNotImplemented {
    testCleanMarc21SplitByNumber(false, 0);
  }

  public void testCleanMarc21NoSplitParallel() throws IOException, StatusNotImplemented {
    testCleanMarc21SplitByNumber(true, 0);
  }

  public void testCleanMarc21Split1Serial() throws IOException, StatusNotImplemented {
    testCleanMarc21SplitByNumber(false, 1);
  }

  public void testCleanMarc21Split1Parallel() throws IOException, StatusNotImplemented {
    testCleanMarc21SplitByNumber(true, 1);
  }

  /*
   * public void testCleanMarc21Split100() throws IOException,
   * StatusNotImplemented { testCleanMarc21SplitByNumber(100); }
   * 
   * public void testCleanMarc21Split1000() throws IOException,
   * StatusNotImplemented { testCleanMarc21SplitByNumber(1000); }
   */
  private void testCleanTurboMarcSplitByNumber(boolean inParallel, int number, boolean clean,
      boolean overwrite, long expected_total) throws IOException, StatusNotImplemented {
    Harvestable resource = createResource(resourceMarc0, "application/marc; charset=MARC8",
	"application/tmarc", 0, number, overwrite);
    resource.setId(2l);
    resource.setTransformation(createTurboMarcTransformation(inParallel));
    RecordStorage recordStorage = createStorage(clean, resource);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), NO_RECORDS, 0, expected_total);
  }

  public void testCleanTurboMarcNoSplitSerial() throws IOException, StatusNotImplemented {
    testCleanTurboMarcSplitByNumber(false, 0, true, true, NO_RECORDS);
  }

  public void testCleanTurboMarcSplit1Parallel() throws IOException, StatusNotImplemented {
    testCleanTurboMarcSplitByNumber(true, 1, true, true, NO_RECORDS);
  }

  public void testCleanTurboMarcSplit100() throws IOException, StatusNotImplemented {
    testCleanTurboMarcSplitByNumber(false, 100, true, true, NO_RECORDS);
  }

  /*
   * public void testCleanTurboMarcSplit1000() throws IOException,
   * StatusNotImplemented { testCleanTurboMarcSplitByNumber(false, 1000, true,
   * true, NO_RECORDS); }
   */

  private void testGZippedMarc21SplitByNumber(boolean inParallel, int number, boolean clean,
      boolean overwrite, long total_expected) throws IOException, StatusNotImplemented {
    Harvestable resource = createResource(resourceMarcGZ, "application/marc", null, 1, number,
	overwrite);
    resource.setId(2l);
    resource.setTransformation(createMarc21Transformation(inParallel));

    RecordStorage recordStorage = createStorage(clean, resource);

    RecordHarvestJob job = doHarvestJob(recordStorage, resource);

    checkStorageStatus(recordStorage.getStatus(), NO_RECORDS, 0, NO_RECORDS);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  private void testCleanGZippedMarc21SplitByNumber(boolean inParallel, int number,
      boolean overwrite, long expected_total) throws IOException, StatusNotImplemented {
    testGZippedMarc21SplitByNumber(inParallel, number, true, overwrite, NO_RECORDS);
  }

  public void testCleanGZippedMarc21NoSplit() throws IOException, StatusNotImplemented {
    testCleanGZippedMarc21SplitByNumber(false, 1, true, NO_RECORDS);
  }

  public void testCleanGZippedMarc21Split1() throws IOException, StatusNotImplemented {
    testCleanGZippedMarc21SplitByNumber(false, 1, true, NO_RECORDS);
  }

  public void testCleanGZippedMarc21Split100() throws IOException, StatusNotImplemented {
    testCleanGZippedMarc21SplitByNumber(false, 100, true, NO_RECORDS);
  }

  public void testCleanGZippedMarc21Split1000() throws IOException, StatusNotImplemented {
    testCleanGZippedMarc21SplitByNumber(false, 1000, true, NO_RECORDS);
  }

  private void testGZippedTurboMarcSplitByNumber(boolean inParallel, int number, boolean clear,
      boolean overwrite, long expected_total) throws IOException, StatusNotImplemented {
    Harvestable resource = createResource(resourceMarcGZ, "application/marc",
	"application/tmarc", 1, number, overwrite);
    resource.setId(2l);
    resource.setTransformation(createTurboMarcTransformation(inParallel));

    RecordStorage recordStorage = createStorage(clear, resource);

    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), NO_RECORDS, 0, expected_total);
  }

  private void testCleanGZippedTurboMarcSplitByNumber(boolean inParallel, int number)
      throws IOException, StatusNotImplemented {
    testGZippedTurboMarcSplitByNumber(inParallel, number, true, true, NO_RECORDS);
  }

  public void testCleanGZippedTurboMarcNoSplit() throws IOException, StatusNotImplemented {
    testCleanGZippedTurboMarcSplitByNumber(false, 0);
  }

  public void testCleanGZippedTurboMarcSplit1() throws IOException, StatusNotImplemented {
    testCleanGZippedTurboMarcSplitByNumber(false, 1);
  }

  public void testCleanGZippedTurboMarcSplit100() throws IOException, StatusNotImplemented {
    testCleanGZippedTurboMarcSplitByNumber(false, 100);
  }

  public void testCleanGZippedTurboMarcSplit1000() throws IOException, StatusNotImplemented {
    testCleanGZippedTurboMarcSplitByNumber(false, 1000);
  }

  public void testUrlGZippedTurboMarc(String url, boolean inParallel, boolean clear,
      boolean overwrite, long expected_total) throws IOException, StatusNotImplemented {
    Harvestable resource = createResource(url, "application/marc; charset=MARC-8",
	"application/tmarc", 1, 100, overwrite);
    resource.setId(2l);
    resource.setTransformation(createTurboMarcTransformation(inParallel));

    RecordStorage recordStorage = createStorage(clear, resource);
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    assertTrue("Job not finished: " + job.getStatus(), job.getStatus() == HarvestStatus.FINISHED);
    assertTrue("resource status wrong: " + resource.getCurrentStatus(), resource.getCurrentStatus().equals(HarvestStatus.FINISHED.name())); 
    checkStorageStatus(recordStorage.getStatus(), NO_RECORDS, 0, expected_total);
  }

  public void testCleanJumpPageGZippedTurboMarc(int number, boolean clear, boolean overwrite,
      long expected_total) throws IOException, StatusNotImplemented {
    testUrlGZippedTurboMarc(resourceMarc0 + " " + resourceMarc1, false, true, true, 2004);
  }

  public void testMultiGZippedTurboMarcTwoJobs() throws IOException, StatusNotImplemented {
    testUrlGZippedTurboMarc(resourceMarc0, false, true, true, NO_RECORDS);
    testUrlGZippedTurboMarc(resourceMarc1, false, false, false, 2 * NO_RECORDS);
  }

  public void testMulti2GZippedTurboMarcFourJobsAndOverwrite() throws IOException,
      StatusNotImplemented {
    testUrlGZippedTurboMarc(resourceMarc0, false, true, true, NO_RECORDS);
    testUrlGZippedTurboMarc(resourceMarc1, false, false, false, 2 * NO_RECORDS);
    testUrlGZippedTurboMarc(resourceMarc2, false, false, false, 3 * NO_RECORDS);
    /* Now restart and check that overwrite mode worked */
    testUrlGZippedTurboMarc(resourceMarc0, false, false, true, NO_RECORDS);

  }

  private void testZippedMarc21SplitByNumber(String zipMarcUrl, boolean inParallel, boolean clean,
      boolean overwrite, long total_expected) throws IOException, StatusNotImplemented {
    Harvestable resource = createResource(zipMarcUrl, "application/marc", null, 1, 1, overwrite);
    resource.setId(2l);
    resource.setTransformation(createMarc21Transformation(inParallel));

    RecordStorage recordStorage = createStorage(clean, resource);

    RecordHarvestJob job = doHarvestJob(recordStorage, resource);

    checkStorageStatus(recordStorage.getStatus(), total_expected, 0, total_expected);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    job.finishReceived();
    assertTrue("resource status wrong: " + resource.getCurrentStatus(), resource.getCurrentStatus().equals(HarvestStatus.FINISHED.name())); 
  }

  private void testZippedMarcXmlSplitByNumber(String zipMarcUrl, boolean inParallel, boolean clean,
      boolean overwrite, int added, long total_expected) throws IOException, StatusNotImplemented {
    Harvestable resource = createResource(zipMarcUrl, null, null, 1, 1, overwrite);
    resource.setId(2l);
    resource.setTransformation(createMarc21Transformation(inParallel));

    RecordStorage recordStorage = createStorage(clean, resource);

    RecordHarvestJob job = doHarvestJob(recordStorage, resource);

    checkStorageStatus(recordStorage.getStatus(), added, 0, total_expected);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testCleanMarc21ZippedSplitBy() throws IOException, StatusNotImplemented {
    testZippedMarc21SplitByNumber(resourceMarcZIP, false, true, true, NO_RECORDS);
  }

  public void testCleanMarc21ZippedMultiEntriesSplitBy() throws IOException, StatusNotImplemented {
    testZippedMarc21SplitByNumber(resourceMarcZIPMulti, false, true, true, 1000);
  }

  public void testCleanMarcXmlZippedMultiEntriesSplitBy() throws IOException, StatusNotImplemented {
    testZippedMarcXmlSplitByNumber(resourceMarcXmlZIPMulti, false, true, true, 10020, 10007);
  }

  @SuppressWarnings("unused")
  private class JobStorageHelper {
    private final StorageStatus expectedStorageStatus;
    private final boolean overwrite;
    private final boolean clean;
    private final boolean inParallel;
    private final String url;
    private final StorageCreator storageCreator;

    public JobStorageHelper(String resourceUrl, boolean isParallel, boolean isClean,
	boolean doOverwrite, StorageCreator storageCreator, StorageStatus expectedStorage)
	throws IOException, StatusNotImplemented {
      url = resourceUrl;
      inParallel = isParallel;
      clean = isClean;
      overwrite = doOverwrite;
      expectedStorageStatus = expectedStorage;
      this.storageCreator = storageCreator;
    }

    public void test() throws IOException, StatusNotImplemented {
      Harvestable resource = createResource(url, null, null, 1, 1, overwrite);
      resource.setId(2l);
      resource.setTransformation(createMarc21Transformation(inParallel));
      RecordStorage recordStorage = storageCreator.createStorage(clean, resource);

      RecordHarvestJob job = doHarvestJob(recordStorage, resource);

      assertTrue(job.getStatus() == HarvestStatus.FINISHED);
      expectedStorageStatus.equals(recordStorage.getStatus());
    }
  }

  public void testBadSolrStorage() throws IOException, StatusNotImplemented {

    Harvestable resource = createResource(resourceMarc0, "application/marc;charset=MARC8",
	null, 1, 1, false);
    resource.setId(2l);
    resource.setTransformation(createMarc21Transformation(false));
    SolrStorageEntity storageEntity = new SolrStorageEntity();
    storageEntity.setName("Bad Storage");
    storageEntity.setId(resource.getId());
    storageEntity.setUrl(solrBadUrl);
    resource.setStorage(storageEntity);
    RecordStorage recordStorage = initializeStorage(false, resource, new BulkSolrRecordStorage(resource));

    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    HarvestStatus jobStatus = job.getStatus();
    assertTrue("Wrong Storage status: " + jobStatus, jobStatus == HarvestStatus.ERROR);
    String errorMessage = resource.getMessage();
    assertTrue("Wrong Error message: " + errorMessage,
	"Solr Server Exception while adding records: Server refused connection at: http://localhost:8686/solrbad"
	    .equals(errorMessage));
  }

  public void testBadSplitAt() throws IOException, StatusNotImplemented {

    XmlBulkResource resource = createResource(resourceMarc0, "application/marc;charset=MARC8",
	null, "", "", false);
    resource.setId(2l);
    resource.setTransformation(createMarc21Transformation(false));
    RecordStorage recordStorage = createStorage(true, resource);
    
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    HarvestStatus jobStatus = job.getStatus();
    assertTrue("Wrong Storage status: " + jobStatus, jobStatus == HarvestStatus.FINISHED);
  }

  
  public void testCleanOAIsterTurboMarcRecordLimit() throws IOException, StatusNotImplemented { 
    Harvestable resource = createResource(resourceMarcUTF8, "application/marc", "application/tmarc",1, 1000, false); 
    int recordLimit = 3 * NO_RECORDS;
    resource.setId(2l);
    resource.setRecordLimit(recordLimit);
    resource.setTransformation(createTurboMarcTransformation(false));

    RecordStorage recordStorage = createStorage(true, resource);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), resource));
    RecordHarvestJob job = doHarvestJob(recordStorage, resource);
    
    StorageStatus storageStatus = recordStorage.getStatus();
    assertTrue(StorageStatus.TransactionState.Committed ==
	storageStatus.getTransactionState()); assertTrue("Deleted records failed "
	    + storageStatus.getDeletes(), new
	    Long(0).equals(storageStatus.getDeletes()));
	assertTrue("Add records failed " + storageStatus.getAdds(), new
	    Long(recordLimit).equals(storageStatus.getAdds())); assertTrue(job.getStatus()
		== HarvestStatus.FINISHED); 
  }
   
/* 
   * public void testCleanOAIsterGZipSplit1000TurboMarc() throws IOException,
   * StatusNotImplemented { XmlBulkResource resource =
   * createResource(resourceMarcUTF8, "application/marc", "application/tmarc",
   * 1, 1000); resource.setId(2l);
   * resource.setTransformation(createTurboMarcTransformation()); RecordStorage
   * recordStorage = createStorage(clean, resource); recordStorage.setLogger(new
   * ConsoleStorageJobLogger(recordStorage.getClass(), resource));
   * RecordHarvestJob job = doHarvestJob(recordStorage, resource);
   * 
   * StorageStatus storageStatus = recordStorage.getStatus();
   * assertTrue(StorageStatus.TransactionState.Committed ==
   * storageStatus.getTransactionState()); assertTrue("Deleted records failed "
   * + storageStatus.getDeletes(), new
   * Long(0).equals(storageStatus.getDeletes()));
   * assertTrue("Add records failed " + storageStatus.getAdds(), new
   * Long(100000).equals(storageStatus.getAdds())); assertTrue(job.getStatus()
   * == HarvestStatus.FINISHED); }
   * 
   * public void testCleanLoCGzSplit1000TurboMarc() throws IOException,
   * StatusNotImplemented { XmlBulkResource resource =
   * createResource(resourceLoCMarc8gz, "application/marc; charset=MARC8",
   * "application/tmarc", 1, 1000); resource.setId(2l);
   * resource.setTransformation(createTurboMarcTransformation()); RecordStorage
   * recordStorage = createStorage(clean, resource); recordStorage.setLogger(new
   * ConsoleStorageJobLogger(recordStorage.getClass(), resource));
   * RecordHarvestJob job = doHarvestJob(recordStorage, resource);
   * 
   * StorageStatus storageStatus = recordStorage.getStatus();
   * assertTrue(StorageStatus.TransactionState.Committed ==
   * storageStatus.getTransactionState()); assertTrue("Deleted records failed "
   * + storageStatus.getDeletes(), new
   * Long(0).equals(storageStatus.getDeletes()));
   * assertTrue("Add records failed " + storageStatus.getAdds(), new
   * Long(250000).equals(storageStatus.getAdds())); assertTrue(job.getStatus()
   * == HarvestStatus.FINISHED); }
   */
}
