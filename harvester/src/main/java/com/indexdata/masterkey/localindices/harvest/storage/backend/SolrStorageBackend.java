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
	protected String indexName;
	private Properties properties;
	//private SolrServer solrSrv = null; 
	private Thread serverThread = null;

	public SolrStorageBackend(String file_url, String idxname) {
		baseDirectory = new File(file_url);
		indexName = idxname;
	}
	
	@Override
	public void init(Properties props) {
		properties = props;
		boolean hasDir = true;
        if (!baseDirectory.exists()) {
            logger.log(Level.INFO, "HARVEST_DIR does not seem to exist, trying to create...");
            hasDir = baseDirectory.mkdir();
        }
        if (!hasDir) {
            logger.log(Level.FATAL, "Cannot access HARVEST_DIR at"
                    + baseDirectory.getAbsolutePath() + ", deployment aborted.");
            return;
        }

        //zebra dirs, configs, etc
        new File(baseDirectory, "reg").mkdir();
        new File(baseDirectory, "shadow").mkdir();
        new File(baseDirectory, "lock").mkdir();
        new File(baseDirectory, "tmp").mkdir();        
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
