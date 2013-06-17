package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Before;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.entity.SolrStorageEntity;
import com.indexdata.masterkey.localindices.harvest.storage.BulkSolrRecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.StatusNotImplemented;
import com.indexdata.masterkey.localindices.harvest.storage.StorageStatus;

public class TestOAIRecordHarvestJob extends JobTester {

  String resourceOaiDcUrl = "http://ir.ub.rug.nl/oai/";
  String resourceOaiDcIso8859_1 = "http://www.intechopen.com/oai/index.php";

  String resourceOaiPubMed = "http://www.pubmedcentral.nih.gov/oai/oai.cgi";
  
  String resourceOAI2MarcUrl = "http://www.diva-portal.org/dice/oai";
  String solrUrl = "http://localhost:8585/solr/";
  //String solrUrl = "http://lui-dev.indexdata.com/solr/";
  RecordStorage recordStorage;
  Logger logger = Logger.getLogger(this.getClass());
  
  @Before
  public void init() {
    BasicConfigurator.configure();
  }
  
  private OaiPmhResource createResource(String url, String prefix, Date from, Date until, String setName, String encoding)
      throws IOException {
    OaiPmhResource resource = new OaiPmhResource();
    resource.setUrl(url);
    resource.setId(1l);
    resource.setCurrentStatus("NEW");
    resource.setEnabled(true);
    // OAI-PMH specific
    if (from != null) {
      resource.setFromDate(from);
    }
    if (until != null) {
      resource.setUntilDate(until);
    }
    resource.setOaiSetName(setName);
    resource.setMetadataPrefix(prefix);
    resource.setEncoding(encoding);
    return resource;
  }

  private RecordHarvestJob doXDaysHarvestJob(RecordStorage recordStorage, OaiPmhResource resource)
      throws IOException {
    OAIRecordHarvestJob job = new OAIRecordHarvestJob(resource, null);
    job.setStorage(recordStorage);
    job.setLogger(new ConsoleStorageJobLogger(job.getClass(), resource));
    job.run();
    return job;
  }

  public void testClean10DaysNoBulkHarvestJob() throws IOException, StatusNotImplemented {
    logger.info("Logging testClean10DaysNoBulkHarvestJob");
    OaiPmhResource resource = createResource(resourceOaiDcUrl, "oai_dc",
	new Date(new Date().getTime() - 10l * 24 * 60 * 60 * 1000), null, null, null);
    resource.setId(1l);
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);
    // checkStorageStatus(recordStorage.getStatus(), 242, 0, 242);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    StorageStatus status = recordStorage.getStatus();
    long adds = status.getAdds();
    long deletes = status.getDeletes();
    long total = status.getTotalRecords();
    System.out.println("Records added: " + adds + ". Deleted: " + deletes + ". Total: " + total);
    assertTrue("Added differs from total " + adds + "!=" + total, adds == total);
  }

  public void testClean1MonthBulkHarvestJob_Overwrite() throws IOException, StatusNotImplemented {
    logger.info("Logging testClean1MonthBulkHarvestJob");
    Date startDate = new GregorianCalendar(2012, 0, 1).getTime();
    Date midDate   = new GregorianCalendar(2012, 1, 1).getTime();
    Date lastDate  = new GregorianCalendar(2012, 2, 1).getTime();
    
    OaiPmhResource resource = createResource(resourceOaiDcUrl, "oai_dc", 
	startDate,
	midDate, null, null);
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), 182, 0, 182);
    Date fromDate = resource.getFromDate();
    assertTrue("FromDate not correct " + fromDate, fromDate.equals(midDate));
    resource.setUntilDate(lastDate);
    job = doXDaysHarvestJob(recordStorage, resource);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), 440, 0, 622);
    resource = createResource(resourceOaiDcUrl, "oai_dc", startDate, midDate, null, null);
    resource.setOverwrite(true);
    job = doXDaysHarvestJob(recordStorage, resource);
    checkStorageStatus(recordStorage.getStatus(), 182, 0, 182);
    fromDate = resource.getFromDate();
    assertTrue("FromDate not correct " + fromDate, fromDate.equals(midDate));
    
  }

  public void testCleanRangeBulkHarvestJob_OaiDC_UTF8() throws IOException, StatusNotImplemented {
    OaiPmhResource resource = createResource(resourceOaiDcIso8859_1, "oai_dc", new GregorianCalendar(2008, 8, 1).getTime(), new GregorianCalendar(2008, 8, 2).getTime(), null, "UTF-8");
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), 109, 0, 109);
  }

  public void testCleanRangeBulkHarvestJob_OaiDC_iso8859_1() throws IOException, StatusNotImplemented {
    OaiPmhResource resource = createResource(resourceOaiDcIso8859_1, "oai_dc", new GregorianCalendar(2008, 8, 1).getTime(), new GregorianCalendar(2008, 8, 2).getTime(), null, "iso-8859-1");
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), 109, 0, 109);

  }

  /*
  public void testCleanFullBulkHarvestJob_OaiDC_iso8859_1() throws IOException {
    OaiPmhResource resource = createResource(resourceOaiDcIso8859_1, "oai_dc", null, null, null, "iso-8859-1");
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }
 */
  public void testClean1MonthBulkHarvestJob_OaiDcPubmed() throws IOException, StatusNotImplemented {
    OaiPmhResource resource = createResource(resourceOaiPubMed, "oai_dc", 
	new GregorianCalendar(2012, 1, 1).getTime(), 
	new GregorianCalendar(2012, 1, 1).getTime(), null, null);
    resource.setId(2l);
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    // checkStorageStatus(recordStorage.getStatus(), 675, 0, 675);
  }


  public void testClean10DaysHarvestJob_OaiMarc21() throws IOException, StatusNotImplemented {
    OaiPmhResource resource = createResource(resourceOAI2MarcUrl, "marc21",
	new Date(new Date().getTime() - 10l * 24 * 60 * 60 * 1000), null, null, null);
    resource.setId(2l);
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    //checkStorageStatus(recordStorage.getStatus(), 1020, 0, 1020);
    StorageStatus status = recordStorage.getStatus();
    long adds = status.getAdds();
    long deletes = status.getDeletes();
    long total = status.getTotalRecords();
    System.out.println("Records added: " + adds + ". Deleted: " + deletes + ". Total: " + total);
    assertTrue("Added differs from total " + adds + "!=" + total, adds == total);
  }

  public void testCleanFullBulkHarvestJob_OaiDc() throws IOException, StatusNotImplemented {
    OaiPmhResource resource = createResource(resourceOAI2MarcUrl, "oai_dc", null, null, "book", null);
    resource.setId(2l);
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), 766, 0, 766);
  }

  public void testCleanFullBulkHarvestJob_OaiMarc21() throws IOException, StatusNotImplemented {
    OaiPmhResource resource = createResource(resourceOAI2MarcUrl, "marc21", null, null, "book", null);
    resource.setId(2l);
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), 766, 0, 766);
  }

  public void testCleanResumptionBulkHarvestJob_OaiMarc21() throws IOException, StatusNotImplemented {
    OaiPmhResource resource = createResource(resourceOAI2MarcUrl, "marc21", null, null, "book",null);
    boolean purge = true;
    resource.setId(2l);
    RecordStorage recordStorage = createStorage(resource, purge);
    OAIRecordHarvestJob job = new OAIRecordHarvestJob(resource, null) {
      int index = 0;
      @Override
      public synchronized boolean isKillSent() {
	index++; 
	if (index % 3 == 0) 
	  kill();
        return super.isKillSent();
      } 
    };
    job.setLogger(new ConsoleStorageJobLogger(job.getClass(), resource));
    job.setStorage(recordStorage);
    job.run();
    assertTrue(resource.getResumptionToken() != null);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), 200, 0, 200);
    // Finish the job
    job = new OAIRecordHarvestJob(resource, null);
    job.setLogger(new ConsoleStorageJobLogger(job.getClass(), resource));
    job.setStorage(recordStorage);
    job.run();
    assertTrue(resource.getResumptionToken() == null);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), 566, 0, 766);
    
  
  }

  private RecordStorage createStorage(Harvestable resource, boolean purge) throws IOException {
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    //RecordStorage recordStorage = resource.getStorage();
    SolrStorageEntity storageEntity = new SolrStorageEntity();
    storageEntity.setId(2l);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), storageEntity));
    recordStorage.setOverwriteMode(resource.getOverwrite());
    // We want an empty storage;
    if (purge) {
      recordStorage.begin();
      recordStorage.purge(true);
    }
    return recordStorage;
  }

}
