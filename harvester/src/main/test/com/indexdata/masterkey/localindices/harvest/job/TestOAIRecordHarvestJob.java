package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Before;

import junit.framework.TestCase;

import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.harvest.storage.BulkSolrRecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.SolrRecordStorage;

public class TestOAIRecordHarvestJob extends TestCase {

  String resourceOaiDcUrl = "http://ir.ub.rug.nl/oai/";
  String resourceOAI2MarcUrl = "http://www.diva-portal.org/dice/oai";
  // String solrUrl = "http://localhost:8080/solr/";
  String solrUrl = "http://lui-dev.indexdata.com/solr/";
  RecordStorage recordStorage;
  Logger logger = Logger.getLogger(this.getClass());
  
  @Before
  public void init() {
    BasicConfigurator.configure();
  }
  
  private OaiPmhResource createResource(String url, String prefix, Date from, Date until)
      throws IOException {
    OaiPmhResource resource = new OaiPmhResource();
    resource.setEnabled(true);
    if (from != null) {
      resource.setFromDate(from);
    }
    if (until != null) {
      resource.setUntilDate(until);
    }
    resource.setMetadataPrefix(prefix);
    resource.setUrl(url);
    resource.setId(1l);
    resource.setCurrentStatus("NEW");
    return resource;
  }

  private RecordHarvestJob doXDaysHarvestJob(RecordStorage recordStorage, OaiPmhResource resource)
      throws IOException {
    RecordHarvestJob job = new OAIRecordHarvestJob(resource, null);
    job.setStorage(recordStorage);
    job.run();
    return job;
  }

  public void testClean10DaysNoBulkHarvestJob() throws IOException {
    logger.info("Logging testClean10DaysNoBulkHarvestJob");
    OaiPmhResource resource = createResource(resourceOaiDcUrl, "oai_dc",
	new Date(new Date().getTime() - 10l * 24 * 60 * 60 * 1000), null);
    resource.setId(1l);
    RecordStorage recordStorage = new SolrRecordStorage(solrUrl, resource);
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testClean1MonthBulkHarvestJob() throws IOException {
    logger.info("Logging testClean1MonthBulkHarvestJob");
    OaiPmhResource resource = createResource(resourceOaiDcUrl, "oai_dc", new GregorianCalendar(2012, 1, 1).getTime(),
	new GregorianCalendar(2012, 2, 1).getTime());
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    assertTrue(resource.getFromDate().equals(new GregorianCalendar(2012, 2, 2).getTime()));
  }

  public void testCleanFullBulkHarvestJob_OaiDC() throws IOException {
    OaiPmhResource resource = createResource(resourceOaiDcUrl, "oai_dc", null, null);
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testClean10DaysHarvestJob_OaiMarc21() throws IOException {
    OaiPmhResource resource = createResource(resourceOAI2MarcUrl, "marc21",
	new Date(new Date().getTime() - 10l * 24 * 60 * 60 * 1000), null);
    resource.setId(2l);
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }

  public void testCleanFullBulkHarvestJob_OaiMarc21() throws IOException {
    OaiPmhResource resource = createResource(resourceOAI2MarcUrl, "marc21", null, null);
    resource.setId(2l);
    RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
    recordStorage.setOverwriteMode(true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
  }
}
