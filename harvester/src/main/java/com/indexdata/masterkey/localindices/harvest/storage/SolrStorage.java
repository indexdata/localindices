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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import com.indexdata.masterkey.localindices.entity.Harvestable;

/**
 * A simple utility class for posting raw updates to a Solr server, 
 * has a main method so it can be run on the command line.
 * 
 */
public class SolrStorage implements HarvestStorage {
	public static final String DEFAULT_POST_URL = "http://localhost:8983/solr/update";
	public static final String POST_ENCODING = "UTF-8";
	public static final String VERSION_OF_THIS_TOOL = "1.2";
	private static final String SOLR_OK_RESPONSE_EXCERPT = "<int name=\"status\">0</int>";

	private static final String DATA_MODE_FILES = "files";
	private static final String DATA_MODE_ARGS = "args";
	private static final String DATA_MODE_STDIN = "stdin";
	private HttpURLConnection urlc;
	OutputStream output; 
	private static final Set<String> DATA_MODES = new HashSet<String>();
	static {
		DATA_MODES.add(DATA_MODE_FILES);
		DATA_MODES.add(DATA_MODE_ARGS);
		DATA_MODES.add(DATA_MODE_STDIN);
	}

	protected URL solrUrl;

	public SolrStorage(Harvestable harvestable) {
		try {
			solrUrl = new URL(System.getProperty("url", DEFAULT_POST_URL));

		} catch (MalformedURLException e) {
			throw new RuntimeException("System Property 'url' is not a valid URL: " + System.getProperty("url", DEFAULT_POST_URL), e);
		}
	}

	public SolrStorage(String url_string, Harvestable harvestable) {
		try {
			solrUrl = new URL(url_string);

		} catch (MalformedURLException e) {
			throw new RuntimeException("'url' is not a valid URL: " + url_string, e);
		}
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
		warn("Make sure your XML documents are encoded in " + POST_ENCODING
				+ ", other encodings are not currently supported");
	}

	/**
	 * Pipes everything from the reader to the writer via a buffer
	 */
	private static void pipe(Reader reader, Writer writer) throws IOException {
		char[] buf = new char[1024];
		int read = 0;
		while ( (read = reader.read(buf) ) >= 0) {
			writer.write(buf, 0, read);
		}
		writer.flush();
	}

	@Override
	public void begin() throws IOException {
		urlc = null;
		urlc = (HttpURLConnection) solrUrl.openConnection();
		try {
			urlc.setRequestMethod("POST");
		} 
		catch (ProtocolException e) {
			throw new IOException("Shouldn't happen: HttpURLConnection doesn't support POST??", e);
		}
		urlc.setDoOutput(true);
		urlc.setDoInput(true);
		urlc.setUseCaches(false);
		urlc.setAllowUserInteraction(false);
		urlc.setRequestProperty("Content-type", "text/xml; charset=" + POST_ENCODING);

		output = urlc.getOutputStream();
		try {
			new OutputStreamWriter(output, POST_ENCODING);
		}
		catch (IOException io) {
		}
	}

	private void readResponse(Writer writer) throws IOException 
	{
		if (writer != null)
			writer.close();
		InputStream in = urlc.getInputStream();
		try {
			Reader reader = new InputStreamReader(in);
			pipe(reader, writer);
			reader.close();
		} catch (IOException e) {
			throw new IOException("IOException while reading response", e);
		} finally {
			if (in != null) 
				in.close();
		}
	}

	@Override
	public void commit() throws IOException 
	{
		StringWriter sw = new StringWriter();
		readResponse(sw);
		warnIfNotExpectedResponse(sw.toString(),SOLR_OK_RESPONSE_EXCERPT);
		// Setup new connection
		begin();
		pipe(new StringReader("<commit/>"), new OutputStreamWriter(output));
		StringWriter sw2 = new StringWriter();	
		readResponse(sw2);
		warnIfNotExpectedResponse(sw.toString(),SOLR_OK_RESPONSE_EXCERPT);
	}

	@Override
	public void rollback() throws IOException {
		StringWriter sw = new StringWriter();
		readResponse(sw);
		// don't send a commit
	}

	@Override
	public void purge() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setOverwriteMode(boolean mode) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean getOverwriteMode() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public OutputStream getOutputStream() {
		return output;
	}
}
