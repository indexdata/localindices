package com.indexdata.masterkey.localindices.harvest.storage;

import java.net.MalformedURLException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;

public class SolrStorageStatus extends AbstractStorageStatus {

  SolrServer server;
  String databaseSelect;
  TransactionState transationState = TransactionState.NoTransaction;
  long outstandingAdds = 0;
  long outstandingDeletes = 0;

  long adds = 0;
  long deletes = 0;

  public SolrStorageStatus(String url, String selectDatabase) throws MalformedURLException {
    server = new HttpSolrServer(url);
    databaseSelect = selectDatabase;
  }

  public SolrStorageStatus(SolrServer server, String selectDatabase) {
    this.server = server;
    databaseSelect = selectDatabase;
  }

  @Override
  public Long getTotalRecords() {
    SolrQuery query = new SolrQuery();
    query.setQuery(databaseSelect);
    try {
      QueryResponse rsp = server.query(query);
      if (rsp.getStatus() == 0) {
	return rsp.getResults().getNumFound();
      }

    } catch (SolrServerException sse) {
      // TODO throw a Storage Exception
    }
    return new Long(-1);
  }

  @Override
  public TransactionState getTransactionState() {
    return transationState;
  }

  @Override
  public Long getOutstandingAdds() {
    return outstandingAdds;
  }

  @Override
  public Long getOutstandingDeletes() {
    return outstandingDeletes;
  }

  @Override
  public Long getAdds() {
    return adds;
  }

  @Override
  public Long getDeletes() {
    return deletes;
  }

  public synchronized long incrementAdd(long add) {
    outstandingAdds += add;
    return outstandingAdds;
  }

  public synchronized long incrementDelete(long delete) {
    outstandingDeletes += delete;
    return outstandingDeletes;
  }

  void setTransactionState(TransactionState state) {
    if (state == TransactionState.Committed) {
      adds = outstandingAdds;
      deletes = outstandingDeletes;
      outstandingAdds = 0;
      outstandingDeletes = 0;
    }
    transationState = state;
  }
}
