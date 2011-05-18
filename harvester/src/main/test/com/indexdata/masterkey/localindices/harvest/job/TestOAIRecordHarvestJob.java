package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.util.Date;

import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.XMLFilter;

import junit.framework.TestCase;

import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.harvest.storage.BulkSolrRecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.Pz2SolrRecordContentHandler;
import com.indexdata.masterkey.localindices.harvest.storage.RecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.SolrRecordStorage;
import com.indexdata.masterkey.localindices.harvest.storage.TransformationChainRecordStorageProxy;

public class TestOAIRecordHarvestJob extends TestCase {
	
	OAIRecordHarvestJob job; 
	String resourceUrl = "http://ir.ub.rug.nl/oai/";
	String solrUrl = "http://localhost:8080/solr/";
	RecordStorage recordStorage; 
	
	
	public void TestClean10DaysHarvestJob() throws IOException {
		OaiPmhResource resource = new OaiPmhResource();
		resource.setEnabled(true);
		// Approx 10 days back
		Date fromDate = new Date(new Date().getTime()-1000*60*60*24*10l);
		resource.setFromDate(fromDate);
		resource.setMetadataPrefix("oai_dc");
		resource.setUrl(resourceUrl);
		setName(resourceUrl);
		resource.setCurrentStatus("NEW");
		job = new OAIRecordHarvestJob(resource, null);
		
		SolrRecordStorage recordStorage = new SolrRecordStorage(solrUrl, resource);
		// Really purge everything
		recordStorage.setDatabase(resourceUrl);
		recordStorage.purge();
		recordStorage.commit();
		
		recordStorage.setOverwriteMode(true); 
		job.setStorage(recordStorage);
		job.run();
	}

	public void TestCleanFullBulkHarvestJob() throws IOException {
		OaiPmhResource resource = new OaiPmhResource();
		resource.setEnabled(true);
		// Approx X days back
		//Date fromDate = new Date(new Date().getTime()-1000*60*60*24*600l );
		//resource.setUntilDate(fromDate);
		resource.setMetadataPrefix("oai_dc");
		resource.setUrl(resourceUrl);
		setName(resourceUrl);
		resource.setCurrentStatus("NEW");
		job = new OAIRecordHarvestJob(resource, null);
		
//		SolrRecordStorage recordStorage = new SolrRecordStorage(solrUrl, resource);
		BulkSolrRecordStorage recordStorage = new BulkSolrRecordStorage(solrUrl, resource);
		// Clean database completely
		recordStorage.purge();
		recordStorage.commit();
		// Really purge everything
		recordStorage.setDatabase(resourceUrl);
		//recordStorage.purge();
		//recordStorage.commit();
		
		//recordStorage.setOverwriteMode(true); 
		job.setStorage(recordStorage);
		job.run();
	}
}
