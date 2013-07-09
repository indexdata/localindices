/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.storage;

import org.apache.solr.client.solrj.SolrServer;

/**
 *
 * @author dennis
 */
public interface SolrServerFactory {
  
  SolrServer create();
  
}
