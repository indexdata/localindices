/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage;

import com.indexdata.masterkey.localindices.entity.Harvestable;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.harvest.job.FileStorageJobLogger;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;

public class EmbeddedSolrServerFactory implements SolrServerFactory {

  String solrDirectory = "./solr";
  public EmbeddedSolrServerFactory(String directory) {
    solrDirectory = directory;
  };
  
  @Override
  public SolrServer create() {
      System.setProperty("solr.solr.home", "./solr");
    File file = new File(".");
    String currentDirectory = file.getAbsolutePath();
    
    CoreContainer.Initializer initializer = new CoreContainer.Initializer();
    CoreContainer coreContainer;
    try {
      coreContainer = initializer.initialize();
      EmbeddedSolrServer embedServer = new EmbeddedSolrServer(coreContainer, "");
      return embedServer;
    } catch (FileNotFoundException ex) {
      Logger.getLogger(EmbeddedSolrServerFactory.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }
}
