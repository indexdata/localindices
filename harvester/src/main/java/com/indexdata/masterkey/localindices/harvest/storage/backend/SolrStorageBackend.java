package com.indexdata.masterkey.localindices.harvest.storage.backend;

import java.io.File;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class SolrStorageBackend implements StorageBackend 
{
	Logger logger =  Logger.getLogger(this.getClass()); 
	protected File baseDirectory;
	// Implement as core or ...
	protected String url = "http://localhost:8080/solr";
	private Properties properties;
	private Thread serverThread = null;

	public SolrStorageBackend(String file_url) {
		url = new Url(file_url);
	}
	
	@Override
	public void init(Properties props) {
	}

	@Override
	public int start() {
        int portNum = Integer.parseInt(properties.getProperty("harvester.zebra.port"));
        logger.log(Level.INFO, "Starting zebrasrv at port " + portNum);
        //solrSrv = new SolrServer(properties.getProperty("harvester.dir"), portNum);
        serverThread = new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
			}
        });
        serverThread.start();
		return 0;
	}

	@Override
	public int stop() {
        serverThread.interrupt();
		return 0;
	}

	@Override
	public boolean isRunning() {
		return serverThread.isAlive();
	}

}
