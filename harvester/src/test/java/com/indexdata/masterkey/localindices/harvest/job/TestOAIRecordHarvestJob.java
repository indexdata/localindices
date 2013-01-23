package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Before;

import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.entity.SolrStorageEntity;
import com.indexdata.masterkey.localindices.harvest.storage.BulkSolrRecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.ConsoleRecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.ConsoleStorage;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;

public class TestOAIRecordHarvestJob extends TestCase {

  String resourceOaiDcUrl = "http://ir.ub.rug.nl/oai/";
  String resourceOaiDcIso8859_1 = "http://www.intechopen.com/oai/index.php";
  
  String resourceOAI2MarcUrl = "http://www.diva-portal.org/dice/oai";
  String solrUrl = "http://localhost:8080/solr/";
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
    resource.setEnabled(true);
    if (from != null) {
      resource.setFromDate(from);
    }
    if (until != null) {
      resource.setUntilDate(until);
    }
    resource.setOaiSetName(setName);
    resource.setMetadataPrefix(prefix);
    resource.setUrl(url);
    resource.setId(1l);
    resource.setCurrentStatus("NEW");
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

  public void testClean10DaysNoBulkHarvestJob() throws IOException {
    logger.info("Logging testClean10DaysNoBulkHarvestJob");
    OaiPmhResource resource = createResource(resourceOaiDcUrl, "oai_dc",
	new Date(new Date().getTime() - 10l * 24 * 60 * 60 * 1000), null, null, null);
    resource.setId(1l);
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testClean1MonthBulkHarvestJob() throws IOException {
    logger.info("Logging testClean1MonthBulkHarvestJob");
    OaiPmhResource resource = createResource(resourceOaiDcUrl, "oai_dc", new GregorianCalendar(2012, 1, 1).getTime(),
	new GregorianCalendar(2012, 2, 1).getTime(), null, null);
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    assertTrue(resource.getFromDate().equals(new GregorianCalendar(2012, 2, 2).getTime()));
  }

  public void testCleanRangeBulkHarvestJob_OaiDC_iso8859_1() throws IOException {
    OaiPmhResource resource = createResource(resourceOaiDcIso8859_1, "oai_dc", new GregorianCalendar(2008, 8, 1).getTime(), new GregorianCalendar(2008, 8, 2).getTime(), null, "iso-8859-1");
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  /*
  public void testCleanFullBulkHarvestJob_OaiDC_iso8859_1() throws IOException {
    OaiPmhResource resource = createResource(resourceOaiDcIso8859_1, "oai_dc", null, null, null, "iso-8859-1");
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }
 */
  public void testClean10DaysHarvestJob_OaiMarc21() throws IOException {
    OaiPmhResource resource = createResource(resourceOAI2MarcUrl, "marc21",
	new Date(new Date().getTime() - 10l * 24 * 60 * 60 * 1000), null, null, null);
    resource.setId(2l);
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testCleanFullBulkHarvestJob_OaiDc() throws IOException {
    OaiPmhResource resource = createResource(resourceOAI2MarcUrl, "oai_dc", null, null, "book", null);
    resource.setId(2l);
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testCleanFullBulkHarvestJob_OaiMarc21() throws IOException {
    OaiPmhResource resource = createResource(resourceOAI2MarcUrl, "marc21", null, null, "book", null);
    resource.setId(2l);
    RecordStorage recordStorage = createStorage(resource, true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testCleanResumptionBulkHarvestJob_OaiMarc21() throws IOException {
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
    // Finish the job
    job = new OAIRecordHarvestJob(resource, null);
    job.setLogger(new ConsoleStorageJobLogger(job.getClass(), resource));
    job.setStorage(recordStorage);
    job.run();
    assertTrue(resource.getResumptionToken() == null);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    
  
  }

  private RecordStorage createStorage(OaiPmhResource resource, boolean purge) throws IOException {
    RecordStorage recordStorage = new ConsoleRecordStorage();
    SolrStorageEntity storageEntity = new SolrStorageEntity();
    storageEntity.setId(2l);
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), storageEntity));
    recordStorage.setOverwriteMode(false);
    // We want an empty storage;
    if (purge) {
      recordStorage.begin();
      recordStorage.purge(true);
    }
    return recordStorage;
  }

}
