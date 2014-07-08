/*
 * Copyright (c) 1995-2013, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.web.admin.controller;

import com.indexdata.masterkey.localindices.entity.HarvestConnectorResource;
import com.indexdata.masterkey.localindices.entity.Setting;
import com.indexdata.utils.XmlUtils;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.model.SelectItem;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static com.indexdata.utils.TextUtils.joinPath;
import java.io.Serializable;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

/**
 *
 * @author jakub
 */
@ManagedBean(name="repoController")
@ViewScoped
public class RepoController implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = -2959742931617525262L;
  private final static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.admin");
  private final static String repoFilterQuery = "?filter=harvest&show_all=0&filter_type=tasks&search=search&xml=1";
  
  @ManagedProperty("#{resourceController}")
  private JobController resourceController;

  //JSF bug -- injection does not work without a setter
  public void setResourceController(JobController resourceController) {
    this.resourceController = resourceController;
  }
  
  @ManagedProperty("#{settings}")
  private SettingsController settingsController;

  //JSF bug -- injection does not work without a setter
  public void setSettingsController(SettingsController settingsController) {
    this.settingsController = settingsController;
  }
  
  
  private String repoUrl; //= "https://idtest:idtest3636@cfrepo-test.indexdata.com/repo.pl/idtest";
  
  private Document cachedRepoResponse;
  
  /**
   * Called when user changes the repoUrl setting.
   * @param e 
   */
  public void repoUrlChanged(ValueChangeEvent e) {
     Setting repoUrlSet = (Setting) e.getNewValue();
     repoUrl = repoUrlSet.getValue();
     logger.info("Repo url setting changed to "+repoUrl);
     //cache it right away
     cachedRepoResponse = performRequest(joinPath(getRepoUrl(), repoFilterQuery));
  }
  
  public String getRepoUrl() {
    if (repoUrl == null) {
      //try retrieving from the bean
      if (resourceController.getResource() instanceof HarvestConnectorResource) {
         Setting repoUrlSetting = ((HarvestConnectorResource) 
           resourceController.getResource()).getConnectorRepoUrlSetting();
        if (repoUrlSetting != null) {
          repoUrl = repoUrlSetting.getValue();
          logger.info("Retrieved repo url setting from resource controller - "+repoUrl);
        } else { //get the first setting available
          // TODO throws null pointer if none is configured
          repoUrl = settingsController.getConnectorRepos().get(0).getValue();
          logger.info("Retrieved repo url setting from settings controller - "+repoUrl);
        }
      }
      cachedRepoResponse = null;
    }
    return repoUrl;
  }

  public void setRepoUrl(String repoUrl) {
    this.repoUrl = repoUrl;
    cachedRepoResponse = null;
  }
  
  public List<SelectItem> getConnectors() {
    if (cachedRepoResponse == null) {
      cachedRepoResponse = performRequest(joinPath(getRepoUrl(), repoFilterQuery));
    }
    //get each connector name
    NodeList connectorNodes = cachedRepoResponse.getElementsByTagName("connector");
    ArrayList<SelectItem> connectors = new ArrayList<SelectItem>(connectorNodes.getLength());
    for (int i=0; i<connectorNodes.getLength(); i++) {
      Element connectorNode = (Element) connectorNodes.item(i);
      Element prodVer = (Element) connectorNode.getElementsByTagName("prodver").item(0);
      if (prodVer == null || prodVer.getTextContent().isEmpty()) //non-production
        continue;
      String title = ((Element) connectorNode
        .getElementsByTagName("title").item(0)).getTextContent();
      String name = ((Element) connectorNode
        .getElementsByTagName("filename").item(0)).getTextContent();
      String author = ((Element) connectorNode
        .getElementsByTagName("author").item(0)).getTextContent();
      NodeList spnl = connectorNode.getElementsByTagName("serviceprovider");
      String serviceProvider = spnl.getLength() > 0 
        ? ((Element) spnl.item(0)).getTextContent()
        : null;
      NodeList notenl = connectorNode.getElementsByTagName("serviceprovider");
      String note = notenl.getLength() > 0 
        ? ((Element) notenl.item(0)).getTextContent()
        : null;
      connectors.add(new SelectItem(
        new ConnectorItem(name, title, serviceProvider, author, note),
        title + " ["+name+"]"));      
    }
    return connectors;
  }
  
   private Document performRequest(String url) {
    logger.info("Retrieving connector list from the repo at '"+url+"'");
    HttpClient hc = new HttpClient();
    //retrieve
    try {
      URI connUri = new URI(url);
      if (connUri.getUserInfo() != null && !connUri.getUserInfo().isEmpty())
        hc.getState().setCredentials(AuthScope.ANY,
          new UsernamePasswordCredentials(connUri.getUserInfo()));
    } catch (URISyntaxException ue) {
      logger.warn("Provided repo URL is wrong! - '"+url);
    }
    HttpMethod hm = new GetMethod(url);
    try {
      int res = hc.executeMethod(hm);
      if (res != 200) {
        logger.warn(res + " returned when connecting to "+url);
        return null;
      }
      if (hm.getResponseHeader("Content-Type").getValue().matches(".*[/+]xml\\b.*"))
        return XmlUtils.parse(hm.getResponseBodyAsStream());
    } catch (IOException ioe) {
      logger.warn("IO error when connecting to "+url+", aborting.");
    } catch (SAXException saxe) {
      logger.warn("XML parsing error from "+url+", aborting.");
    } finally {
      hm.releaseConnection();
    }
    return null;
  }
  
  
}
