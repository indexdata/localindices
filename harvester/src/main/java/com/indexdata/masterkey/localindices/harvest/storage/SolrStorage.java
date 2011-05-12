package com.indexdata.masterkey.localindices.harvest.storage;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;

import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import com.indexdata.masterkey.localindices.entity.Harvestable;

/**
 * A simple utility class for posting raw updates to a Solr server, 
 * has a main method so it can be run on the command line.
 * 
 */
public class SolrStorage implements HarvestStorage {
	public  String POST_ENCODING = "UTF-8";
	public  String VERSION_OF_THIS_TOOL = "1.2";
	protected String url = "http://localhost:8983/solr/";
	protected CommonsHttpSolrServer server;
	protected Harvestable harvestable;
	protected Logger logger = Logger.getLogger(this.getClass());
	ByteArrayOutputStream output = new ByteArrayOutputStream();
	protected Collection<SolrInputDocument> documentList = null;
	protected URL solrUrl;
	private boolean override = false;

	public void init() {
		try {
			
			server = new CommonsHttpSolrServer( url );
			server.setSoTimeout(1000);  // socket read timeout
			server.setConnectionTimeout(100);
			server.setDefaultMaxConnectionsPerHost(100);
			server.setMaxTotalConnections(100);
			server.setFollowRedirects(false);  // defaults to false
			// allowCompression defaults to false.
			  // Server side must support gzip or deflate for this to have any effect.
			server.setAllowCompression(true);
			server.setMaxRetries(1); // defaults to 0.  > 1 not recommended.
			server.setParser(new XMLResponseParser());	

		} catch (MalformedURLException e) {
			throw new RuntimeException("'url' is not a valid URL: " + System.getProperty("url", url), e);
		}
		
	}
	
	public SolrStorage(Harvestable harvestable) {
		this.harvestable = harvestable;
		init();
	}

	public SolrStorage(String url_string, Harvestable harvestable) {
		this.harvestable = harvestable;
		url = url_string;
		init();
	}

	/** Check what Solr replied to a POST, and complain if it's not what we expected.
	 *  TODO: parse the response and check it XMLwise, here we just check it as an unparsed String  
	 */
	static void warnIfNotExpectedResponse(String actual,String expected) {
		if(actual.indexOf(expected) < 0) {
			warn("Unexpected response from Solr: '" + actual + "' does not contain '" + expected + "'");
		}
	}

	static void warn(String msg) {
		System.err.println("SimplePostTool: WARNING: " + msg);
	}

	static void info(String msg) {
		System.out.println("SimplePostTool: " + msg);
	}

	static void fatal(String msg) {
		System.err.println("SimplePostTool: FATAL: " + msg);
		System.exit(1);
	}

	/**
	 * Constructs an instance for posting data to the specified Solr URL 
	 * (ie: "http://localhost:8983/solr/update")
	 */
	public SolrStorage(URL solrUrl, Harvestable harvestable) {
		this.solrUrl = solrUrl;
/*
		warn("Make sure your XML documents are encoded in " + POST_ENCODING
				+ ", other encodings are not currently supported");
*/
	}

	@Override
	public void begin() throws IOException {

		documentList = new LinkedList<SolrInputDocument>();
		output.reset();
		
	}

	@Override
	public void commit() throws IOException  
	{
		SolrXmlParser parser = new SolrXmlParser();
		SolrXmlHandler context = new SolrXmlHandler();
		try {
			parser.parse(output.toString(), context);
		} catch (XMLStreamException e) {
			e.printStackTrace();
			throw new IOException("Error in SOLR XML parse: " + e.getMessage(), e);
		}
		try {
			System.out.println(context.getDocuments());
			UpdateResponse response = server.add(context.getDocuments());
			logger.debug("UpdateResponse: " + response.getStatus() + " " + response.getResponse());
			response = server.commit();
			logger.debug("CommitResponse: " + response.getStatus() + " " + response.getResponse());
			
		} catch (SolrServerException e) {
			throw new IOException("Error in SOLR commit", e);
		}
	}

	@Override
	public void rollback() throws IOException {
		try {
			server.rollback();
		} catch (SolrServerException e) {
			e.printStackTrace();
			throw new RuntimeException("Solr Server Exception in SOLR Rollback: " + e.getMessage(), e);
		}
	}

	@Override
	public void purge() throws IOException {
		try {
			server.deleteByQuery( "database:" + harvestable.getId());
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IOException("Error purging records from server", e);
		}
	}

	@Override
	public void setOverwriteMode(boolean mode) {
		override  = mode;
	}

	@Override
	public boolean getOverwriteMode() {
		return override;
	}

	@Override
	public OutputStream getOutputStream() {		
		return output;
	}
}
