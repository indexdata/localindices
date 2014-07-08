package com.indexdata.masterkey.localindices.web.admin.controller;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.web.admin.utils.CompletionItemsSource;

@ManagedBean(name = "connectorItemsSource")
@ViewScoped
public class ConnectorItemsSource extends CompletionItemsSource {

  private Logger logger = Logger.getLogger(ConnectorItemsSource.class);
  String repoUrl = "";
  List<SelectItem> itemcache = null;
  
  @ManagedProperty("#{repoController}")
  private RepoController repoController;

  public void setRepoController(RepoController repoController) {
    this.repoController = repoController;
  }

  public ConnectorItemsSource() {}

  @Override
  public List<SelectItem> getSelectItems() {
    if (!repoUrl.equals(repoController.getRepoUrl())) {
      logger.debug("getSelectItems(): New repo URL - fetching items");
      repoUrl = repoController.getRepoUrl();
      itemcache = repoController.getConnectors();
      logger.debug("Fetched " + itemcache.size() + "items from " + repoUrl);
    } else {
      logger.debug("getSelectItems(): URL still " + repoUrl + ", returning cached list ");
    }
    return itemcache;
  }

}
