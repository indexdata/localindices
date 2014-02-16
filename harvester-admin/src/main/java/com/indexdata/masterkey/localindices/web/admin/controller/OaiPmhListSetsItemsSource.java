package com.indexdata.masterkey.localindices.web.admin.controller;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.web.admin.controller.lookups.OaiPmhLookupsController;
import com.indexdata.masterkey.localindices.web.admin.utils.CompletionItemsSource;

@ManagedBean(name = "oaiPmhListSetsItemsSource")
@ViewScoped
public class OaiPmhListSetsItemsSource extends CompletionItemsSource {

  private static Logger logger = Logger.getLogger(OaiPmhListSetsItemsSource.class);
  private String resourceUrl = "";
  private List<SelectItem> selectItems = new ArrayList<SelectItem>();
  
  @ManagedProperty("#{resourceController}")
  private ResourceController resourceController;

  public void setResourceController(ResourceController resourceController) {
    logger.debug("Setting resource to " + resourceController);
    this.resourceController = resourceController;
  }
  
  @ManagedProperty("#{oaiPmhLookupsController}")
  private OaiPmhLookupsController oaiPmhLookupsController;

  public void setOaiPmhLookupsController(OaiPmhLookupsController oaiPmhLookupsController) {
    this.oaiPmhLookupsController = oaiPmhLookupsController;
  }
  
  public OaiPmhListSetsItemsSource() {}

  /**
   * Returns cached select items list, unless the resource URL changed on the
   * controller in which case the list is regenerated from the source. 
   */
  @Override
  public List<SelectItem> getSelectItems() {
    String controllerUrl = ((OaiPmhResource)(resourceController.getResource())).getUrl();
    if (resourceUrl == null || !resourceUrl.equals(controllerUrl)) {
      resourceUrl = controllerUrl;
      if (resourceUrl != null) {
        oaiPmhLookupsController.setOaiRepositoryUrl(resourceUrl);
        selectItems = oaiPmhLookupsController.getSetSelectItems();
      }
    }
    return selectItems;
  }

}
