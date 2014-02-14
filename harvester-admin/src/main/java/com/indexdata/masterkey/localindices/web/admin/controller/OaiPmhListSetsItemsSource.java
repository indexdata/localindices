package com.indexdata.masterkey.localindices.web.admin.controller;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.web.admin.controller.lookups.OaiPmhLookups;
import com.indexdata.masterkey.localindices.web.admin.controller.lookups.Set;
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
  
  @ManagedProperty("#{oaiPmhLookups}")
  private OaiPmhLookups oaiPmhLookups;

  public void setOaiPmhLookups(OaiPmhLookups oaiPmhLookups) {
    this.oaiPmhLookups = oaiPmhLookups;
  }
  
  public OaiPmhListSetsItemsSource() {}

  /**
   * 
   */
  @Override
  public List<SelectItem> getSelectItems() {
    String controllerUrl = ((OaiPmhResource)(resourceController.getResource())).getUrl();
    if (resourceUrl == null || !resourceUrl.equals(controllerUrl)) {
      resourceUrl = controllerUrl;
      if (resourceUrl != null) {
        oaiPmhLookups.setOaiRepositoryUrl(resourceUrl);
        selectItems = new ArrayList<SelectItem>();
        List<Set> sets = oaiPmhLookups.getSets();
        for (Set set : sets) {
          selectItems.add(new SelectItem(set.getSetSpec(),set.getSetName()));
        }
      }
    }
    return selectItems;
  }
  
  /**
   * For early caching (before request for select items), 
   * provided a change event is sent. Whatever resource URL is set on the
   * controller takes precedence however, ref. getSelectItems().
   * 
   * @param e
   */
  public void resourceUrlChanged(ValueChangeEvent e) {    
    String newUrl = e.getNewValue().toString();
    if (resourceUrl == null || !resourceUrl.equals(newUrl)) {
      resourceUrl = newUrl;
      oaiPmhLookups.setOaiRepositoryUrl(resourceUrl);
      selectItems = new ArrayList<SelectItem>();
      List<Set> sets = oaiPmhLookups.getSets();
      for (Set set : sets) {
        selectItems.add(new SelectItem(set.getSetSpec(),set.getSetName()));
      }
    }
  }

}
