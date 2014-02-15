package com.indexdata.masterkey.localindices.web.admin.controller;

import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.entity.OaiPmhResource;
import com.indexdata.masterkey.localindices.web.admin.controller.lookups.OaiPmhLookups;
import com.indexdata.masterkey.localindices.web.admin.controller.lookups.OaiPmhResourceException;
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
   * Returns cached select items list, unless the resource URL changed on the
   * controller in which case the list is regenerated from the source. 
   */
  @Override
  public List<SelectItem> getSelectItems() {
    String controllerUrl = ((OaiPmhResource)(resourceController.getResource())).getUrl();
    if (resourceUrl == null || !resourceUrl.equals(controllerUrl)) {
      resourceUrl = controllerUrl;
      if (resourceUrl != null) {
        oaiPmhLookups.setOaiRepositoryUrl(resourceUrl);
        selectItems = new ArrayList<SelectItem>();
        
        try {
          List<Set> sets = oaiPmhLookups.getSets();
          for (Set set : sets) {
            selectItems.add(new SelectItem(set.getSetSpec(),set.getSetName()));
          }
        } catch (OaiPmhResourceException e) {
          setMessage(null,"There was a problem with the resource at " + resourceUrl,e.getMessage());        }
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
      try {
        List<Set> sets = oaiPmhLookups.getSets();
        for (Set set : sets) {
          selectItems.add(new SelectItem(set.getSetSpec(),set.getSetName()));
        }
      } catch (OaiPmhResourceException e1) {
        setMessage(e.getComponent().getClientId(),e1.getMessage(),"Warnign: Failed to retrieve Set Names from the given repository URL");
      }
    }
  }
  
  private void setMessage (String clientId, String summary, String detail) {
    FacesContext context = FacesContext.getCurrentInstance();
    context.addMessage(clientId, 
        new FacesMessage(FacesMessage.SEVERITY_WARN,summary, detail));
  }

}
