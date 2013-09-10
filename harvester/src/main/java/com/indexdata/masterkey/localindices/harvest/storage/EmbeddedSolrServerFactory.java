/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.core.CoreContainer;

public class EmbeddedSolrServerFactory implements SolrServerFactory {
  static JettySolrRunner jettyServer;
  String solrDirectory = "./solr";

  public EmbeddedSolrServerFactory(String directory) {
    solrDirectory = directory;
  };

  public SolrServer createEmbeded() {
    System.setProperty("solr.solr.home", "./solr");
    @SuppressWarnings("unused")
    File file = new File(".");

    CoreContainer.Initializer initializer = new CoreContainer.Initializer();
    CoreContainer coreContainer;
    try {
      coreContainer = initializer.initialize();
      EmbeddedSolrServer embedServer = new EmbeddedSolrServer(coreContainer, "");
      return embedServer;
    } catch (Exception ex) {
      Logger.getLogger(EmbeddedSolrServerFactory.class.getName()).log(Level.SEVERE, "Failed to crete embedded Solr" , ex);
    }
    return null;
  }

  public SolrServer create() {
    // System.setProperty("solr.solr.home", "./solr");
    try {
      if (jettyServer == null) {
	URL url = new URL(solrDirectory);
	String path = url.getPath();
	if (path.endsWith("/")) {
	  path = path.substring(0, path.length() - 1);
	}
	jettyServer = new JettySolrRunner("./solr", path, url.getPort());
	jettyServer.start(true);
      }
      ;
    } catch (Throwable ex) {
      Logger.getLogger(EmbeddedSolrServerFactory.class.getName()).log(Level.SEVERE, "Failed to create JettySolrRunner", ex);
    }
    SolrServer server = new HttpSolrServer(solrDirectory);
    return server;
  }

}
