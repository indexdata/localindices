package com.indexdata.masterkey.localindices.web.admin.controller.lookups;

import java.io.Serializable;
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
import com.indexdata.masterkey.localindices.web.admin.controller.ResourceController;

@ManagedBean(name = "oaiPmhLookupsController")
@ViewScoped
public class OaiPmhLookupsController implements Serializable {

  private OaiPmhLookups lookups = new OaiPmhLookups();
  private static Logger logger = Logger.getLogger(OaiPmhLookupsController.class);
  private String resourceUrl = "";
  private List<SelectItem> setSelectItems = new ArrayList<SelectItem>();
  private List<SelectItem> metadataFormatSelectItems = new ArrayList<SelectItem>();
  Sets sets = new Sets();
  MetadataFormats metadataFormats = new MetadataFormats();
  com.indexdata.masterkey.localindices.web.admin.controller.lookups.Identify identify 
     = new com.indexdata.masterkey.localindices.web.admin.controller.lookups.Identify();
  /**
   * 
   */
  private static final long serialVersionUID = 4673759604059297999L;
  
  @ManagedProperty("#{resourceController}")
  public ResourceController resourceController;
  
  public void setResourceController(ResourceController resourceController) {
    this.resourceController = resourceController;
  }
  
  public ResourceController getResourceController () {
    return this.resourceController;
  }
  

  public OaiPmhLookupsController() {
    // TODO Auto-generated constructor stub
  }

  public void setOaiRepositoryUrl (String oaiRepositoryUrl) {
    logger.debug("Setting OAI URL to " + oaiRepositoryUrl);
    if (resourceUrl == null || !resourceUrl.equals(oaiRepositoryUrl)) {
      this.resourceUrl = oaiRepositoryUrl;
      this.sets = new Sets();
      this.metadataFormats = new MetadataFormats();
      this.identify = new com.indexdata.masterkey.localindices.web.admin.controller.lookups.Identify();
      try {
        loadSetSelectItems();
        loadMetadataFormats();
        loadIdentify();
        OaiPmhResource resource = ((OaiPmhResource) resourceController.getResource());
        if (resource.getDateFormat()!=null) {
          setLongDate(resource.getDateFormat().length()>10);
        } else {
          getLongDate();
        }
      } catch (OaiPmhResourceException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
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
    logger.debug("resourceUrl is ["+ resourceUrl+"], will be: ["+newUrl+"]");
    setOaiRepositoryUrl(newUrl);
  }
  
  public List<SelectItem> getSetSelectItems () {
    return setSelectItems;
  }
  
  public List<SelectItem> getMetadataFormatSelectItems () {
    return metadataFormatSelectItems;
  }
  
  public Identify getIdentify () {
    return identify;
  }
  
  private void loadSetSelectItems() throws OaiPmhResourceException {
    setSelectItems = new ArrayList<SelectItem>();
    List<Set> sets = lookups.getSets(resourceUrl);
    for (Set set : sets) {
      setSelectItems.add(new SelectItem(set.getSetSpec(),set.getSetName()));
    }
  }
  
  private void loadMetadataFormats() throws OaiPmhResourceException  {
    metadataFormatSelectItems = new ArrayList<SelectItem>();
    List<MetadataFormat> metadataFormats = lookups.getMetadataFormats(resourceUrl);
    for (MetadataFormat metadataFormat : metadataFormats) {
      metadataFormatSelectItems.add(new SelectItem(metadataFormat.getMetadataPrefix(),metadataFormat.getMetadataPrefix()));
    }
  }
  
  private void loadIdentify () throws OaiPmhResourceException {
    identify = new com.indexdata.masterkey.localindices.web.admin.controller.lookups.Identify();
    identify = lookups.getIdentify(resourceUrl);
  }
  
  public Boolean getLongDate () {
    logger.debug("Getting long date");
    OaiPmhResource resource = ((OaiPmhResource) resourceController.getResource());
    if (resource.getDateFormat() == null) {
      logger.debug("Getting long date: resource.getDateFormat() is null" );
      logger.debug("identify.getGranularity() is " + identify.getGranularity());
      if (identify.getGranularity().length() > 0) {
        resource.setDateFormat(identify.getGranularity());
        if (resource.getDateFormat().length()>10) {
          resourceController.setLongDate(true);
        } else {
          resourceController.setLongDate(false);
        }
      }
    } else {
      logger.debug("Getting long date: resource.getDateformat() is " + resource.getDateFormat());
    }
    logger.debug("Getting long date: " + resourceController.getLongDate());
    return resourceController.getLongDate();
  }
  
  public void setLongDate(Boolean useLongDate) {
    if (identify.getGranularity().length()<=10) {
      logger.debug("Setting long date to false (not supported)");
      resourceController.setLongDate(false);
    } else {
      logger.debug("Setting long date to " + useLongDate);
      resourceController.setLongDate(useLongDate);
    }
  }

  
  private void setMessage (String clientId, String summary, String detail) {
    FacesContext context = FacesContext.getCurrentInstance();
    context.addMessage(clientId, 
        new FacesMessage(FacesMessage.SEVERITY_WARN,summary, detail));
  }


}
