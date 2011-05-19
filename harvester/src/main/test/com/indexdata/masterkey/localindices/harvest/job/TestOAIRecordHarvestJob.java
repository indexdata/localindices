package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.util.Date;

import junit.framework.TestCase;

import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.harvest.storage.BulkSolrRecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.SolrRecordStorage;

public class TestOAIRecordHarvestJob extends TestCase {
	
	String resourceOaiDcUrl = "http://ir.ub.rug.nl/oai/";
	String resourceOAI2MarcUrl = "http://www.diva-portal.org/dice/oai";
	String solrUrl = "http://localhost:8080/solr/";
	RecordStorage recordStorage; 

	private OaiPmhResource createResource(String url, String prefix, Date from, Date until) throws IOException {
		OaiPmhResource resource = new OaiPmhResource();
		resource.setEnabled(true);
		if (from != null) {
			resource.setFromDate(from);
		}
		if (until != null) {
			resource.setUntilDate(until);
		}
		resource.setMetadataPrefix(prefix);
		resource.setUrl(resourceOaiDcUrl);
		resource.setId(1l);
		resource.setCurrentStatus("NEW");
		return resource;
	}

	private RecordHarvestJob doXDaysHarvestJob(RecordStorage recordStorage, OaiPmhResource resource) throws IOException {

		RecordHarvestJob job = new OAIRecordHarvestJob(resource, null);
		job.setStorage(recordStorage);
		job.run();
		return job;
	}

	
	@SuppressWarnings("deprecation")
	public void TestClean1MonthNoBulkHarvestJob() throws IOException {
	
		OaiPmhResource resource = createResource(resourceOaiDcUrl, "oai_dc", new Date(2011,1, 1), new Date(2011, 2, 1));
		RecordStorage recordStorage = new SolrRecordStorage(solrUrl, resource);
		recordStorage.setOverwriteMode(true);
		RecordHarvestJob job  = doXDaysHarvestJob(recordStorage, resource);

		assert(job.getStatus() == HarvestStatus.FINISHED);
	}

	@SuppressWarnings("deprecation")
	public void TestClean1MonthHarvestJob() throws IOException {
	
		OaiPmhResource resource = createResource(resourceOaiDcUrl, "oai_dc", new Date(2011,1, 1), new Date(2011, 2, 1));
		RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
		recordStorage.setOverwriteMode(true);
		RecordHarvestJob job  = doXDaysHarvestJob(recordStorage, resource);

		assert(job.getStatus() == HarvestStatus.FINISHED);
	}

	public void TestCleanFullBulkHarvestJob() throws IOException {
		OaiPmhResource resource = createResource(resourceOaiDcUrl, "oai_dc", null, null);
		RecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
		recordStorage.setOverwriteMode(true);
		RecordHarvestJob job  = doXDaysHarvestJob(recordStorage, resource);

		assert(job.getStatus() == HarvestStatus.FINISHED);
	}
}
