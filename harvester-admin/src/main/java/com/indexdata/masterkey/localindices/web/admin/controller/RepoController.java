/*
 * Copyright (c) 1995-2013, Index Datassss
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.web.admin.controller;

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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author jakub
 */
@ManagedBean(name="repoController")
public class RepoController {
  private final static Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices.admin");
  private final static String repoUrl = "https://idtest:idtest3636@cfrepo-test.indexdata.com/repo.pl/idtest?filter=harvest&show_all=0&filter_type=tasks&search=search&xml=1";
  
  private String connector;

  public String getConnector() {
    return connector;
  }

  public void setConnector(String connector) {
    this.connector = connector;
  }

  public List<SelectItem> getConnectors() {
    Document resp = performRequest(repoUrl);
    //get each connector name
    NodeList connectorNodes = resp.getElementsByTagName("connector");
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
      connectors.add(new SelectItem(name, title + " ["+name+"]"));      
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
