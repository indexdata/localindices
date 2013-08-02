package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

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
  String solrBadUrl = "http://localhost:8686/solrbad/";
  Logger logger = Logger.getLogger(this.getClass());
  long resourceId = 0l;

  @Before
  public void init() {
    BasicConfigurator.configure();
  }

  private OaiPmhResource createResource(String url, String prefix, Date from, Date until, String setName, String encoding)
          throws IOException {
    OaiPmhResource resource = new OaiPmhResource();
    resource.setUrl(url);
    resource.setName(url);
    resource.setId(++resourceId);
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

  public void testClean10DaysNoOaiPmhJob() throws IOException, StatusNotImplemented {
    logger.info("Logging testClean10DaysNoOaiPmhJob");
    OaiPmhResource resource = createResource(resourceOaiDcUrl, "oai_dc",
            new Date(new Date().getTime() - 10l * 24 * 60 * 60 * 1000), null, null, null);
    RecordStorage recordStorage = createStorage(resource, "testClean10DaysNoOaiPmhJob", true);
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

  public Date createUTCDate(int year, int month, int day) {
    TimeZone utc = TimeZone.getTimeZone("UTC");
    GregorianCalendar cal = new GregorianCalendar(utc);
    cal.set(year, month - 1, day);
    return cal.getTime();
  }

  public void testClean1MonthOaiPmhJob_Overwrite() throws IOException, StatusNotImplemented {
    logger.info("Logging testClean1MonthOaiPmhJob_overwrite");
    Date startDate = createUTCDate(2012, 1, 1);
    Date midDate   = createUTCDate(2012, 2, 1);
    Date lastDate  = createUTCDate(2012, 3, 1);

    OaiPmhResource resource = createResource(resourceOaiDcUrl, "oai_dc", startDate, midDate, null, null);
    RecordStorage recordStorage = createStorage(resource, "testClean1MonthOaiPmhJob_Overwrite", true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), 185, 0, 185);
    Date fromDate = resource.getFromDate();
    assertTrue("FromDate not correct " + fromDate, fromDate.equals(midDate));
    resource.setUntilDate(lastDate);
    job = doXDaysHarvestJob(recordStorage, resource);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), 439, 0, 624);
    resource.setFromDate(startDate);
    resource.setUntilDate(midDate);
    resource.setOverwrite(true);
    job = doXDaysHarvestJob(recordStorage, resource);
    checkStorageStatus(recordStorage.getStatus(), 185, 0, 185);
    fromDate = resource.getFromDate();
    assertTrue("FromDate not correct " + fromDate, fromDate.equals(midDate));

  }

  public void testCleanRangeOaiPmhJob_OaiDC_UTF8() throws IOException, StatusNotImplemented {
    OaiPmhResource resource = createResource(resourceOaiDcUrl, "oai_dc", createUTCDate(2008, 8, 1), createUTCDate(2008, 8, 2), null, "UTF-8");
    RecordStorage recordStorage = createStorage(resource, "testCleanRangeOaiPmhJob_OaiDC_UTF8", true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), 73, 0, 73);
  }

  public void testCleanRangeOaiPmhJob_OaiDC_iso8859_1() throws IOException, StatusNotImplemented {
    OaiPmhResource resource = createResource(resourceOaiDcIso8859_1, "oai_dc", createUTCDate(2008, 8, 1), createUTCDate(2008, 8, 2), null, "iso-8859-1");
    RecordStorage recordStorage = createStorage(resource, "testCleanRangeOaiPmhJob_OaiDC_iso8859_1", true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), 73, 0, 73);

  }

  /*
   public void testCleanFullOaiPmhJob_OaiDC_iso8859_1() throws IOException {
   OaiPmhResource resource = createResource(resourceOaiDcIso8859_1, "oai_dc", null, null, null, "iso-8859-1");
   RecordStorage recordStorage = createStorage(resource, true);
   RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

   assertTrue(job.getStatus() == HarvestStatus.FINISHED);
   }
   */
  public void testClean2DaysMonthOaiPmhJob_OaiDcPubmed() throws IOException, StatusNotImplemented {
    OaiPmhResource resource = createResource(resourceOaiPubMed, "oai_dc",
            createUTCDate(2012, 1, 2),
            createUTCDate(2012, 1, 3), null, null);
    RecordStorage recordStorage = createStorage(resource, "testClean1MonthOaiPmhJob_OaiDcPubmed", true);

    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    checkStorageStatus(recordStorage.getStatus(), 858, 0, 858); // Previous test 2011-11-31 until 2012-01-31 gave 675
  }

  public void testClean10DaysOaiPmhJob_OaiMarc21() throws IOException, StatusNotImplemented {
    OaiPmhResource resource = createResource(resourceOAI2MarcUrl, "marc21",
            new Date(new Date().getTime() - 10l * 24 * 60 * 60 * 1000), null, null, null);
    RecordStorage recordStorage = createStorage(resource, "testClean10DaysOaiPmhJob_OaiMarc21", true);
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

  private void testCleanFullOaiPmhJobMetadataPrefix_Book(String prefix) throws IOException, StatusNotImplemented {
    StackTraceElement stackElement = Thread.currentThread().getStackTrace()[1];
    String methodName = stackElement.getMethodName();
    OaiPmhResource resource = createResource(resourceOAI2MarcUrl, prefix, null, null, "book", null);
    RecordStorage recordStorage = createStorage(resource, methodName, true);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    StorageStatus firstRun = recordStorage.getStatus();
    // Rerun without reusing
    resource = createResource(resourceOAI2MarcUrl, prefix, null, null, "book", null);
    recordStorage = createStorage(resource, methodName, true);
    job = doXDaysHarvestJob(recordStorage, resource);
    assertTrue(job.getStatus() == HarvestStatus.FINISHED);
    StorageStatus secondRun = recordStorage.getStatus();
    assertTrue("First run differs from second run: ", firstRun.equals(secondRun));
  }

  public void testCleanFullOaiPmhJob_Book() throws IOException, StatusNotImplemented {
    testCleanFullOaiPmhJobMetadataPrefix_Book("oai_dc");  
    testCleanFullOaiPmhJobMetadataPrefix_Book("marc21");  
  }

  public void testCleanFullOaiPmhJob_OaiMarc21_BadStorage() throws IOException, StatusNotImplemented {
    OaiPmhResource resource = createResource(resourceOAI2MarcUrl, "marc21", null, null, "book", null);
    RecordStorage recordStorage = createCustomStorage(resource, "testCleanFullOaiPmhJob_OaiMarc21", solrBadUrl, false);
    RecordHarvestJob job = doXDaysHarvestJob(recordStorage, resource);

    assertTrue("Status is not ERROR but " + job.getStatus(), job.getStatus() == HarvestStatus.ERROR);
    System.out.println(resource.getMessage());
  }

  public void testCleanResumptionOaiPmhJob_OaiMarc21() throws IOException, StatusNotImplemented {
    OaiPmhResource resource = createResource(resourceOAI2MarcUrl, "marc21", null, createUTCDate(2013, 6, 8), "book", null);
    boolean purge = true;
    RecordStorage recordStorage = createStorage(resource, "testCleanResumptionOaiPmhJob_OaiMarc21", purge);
    OAIRecordHarvestJob job = new OAIRecordHarvestJob(resource, null) {
      int index = 0;

      @Override
      public synchronized boolean isKillSent() {
        index++;
        if (index % 3 == 0) {
          kill();
        }
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
    // TODO Make test return fix count. 
    checkStorageStatus(recordStorage.getStatus(), 550, 0, 750);


  }

  private RecordStorage createCustomStorage(Harvestable resource, String storageName, String url, boolean clear) throws IOException {
    RecordStorage recordStorage = new BulkSolrRecordStorage(url, resource);
    SolrStorageEntity storageEntity = new SolrStorageEntity();
    storageEntity.setName(storageName);
    storageEntity.setId(resource.getId());
    recordStorage.setLogger(new ConsoleStorageJobLogger(recordStorage.getClass(), storageEntity));
    if (clear) {
      recordStorage.begin();
      recordStorage.purge(true);
      recordStorage.commit();
    }
    return recordStorage;
  }

  private RecordStorage createStorage(Harvestable resource, String storageName, boolean clear) throws IOException {
    return createCustomStorage(resource, storageName, solrUrl, clear);
  }

}
