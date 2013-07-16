/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.job;

import java.io.IOException;
import java.net.Proxy;

import javax.persistence.Entity;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.w3c.dom.NodeList;

import ORG.oclc.oai.harvester2.transport.ResponseParsingException;
import ORG.oclc.oai.harvester2.verb.HarvesterVerb;
import ORG.oclc.oai.harvester2.verb.Identify;

/**
 *
 * @author dennis
 */
@Entity
public class TestOaiPmhIdentify extends TestCase {
  String urlLocal = "http://localhost:8080/harvester/?verb=Identify";
  String url = "http://ir.ub.rug.nl/oai/";
  
  public void testIdentify() throws IOException, ParserConfigurationException, ResponseParsingException, TransformerException {
      HarvesterVerb verb = new Identify(url, Proxy.NO_PROXY, null, Logger.getLogger(this.getClass()));
      
      NodeList nodes = verb.getNodeList("/");
      
  }
}
