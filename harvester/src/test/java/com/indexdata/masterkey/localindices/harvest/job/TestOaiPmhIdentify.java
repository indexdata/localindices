/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.harvest.job;

import ORG.oclc.oai.harvester2.transport.ResponseParsingException;
import ORG.oclc.oai.harvester2.verb.HarvesterVerb;
import ORG.oclc.oai.harvester2.verb.Identify;
import java.io.Serializable;
import java.net.Proxy;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import ORG.oclc.oai.harvester2.verb.Identify;
import ORG.oclc.oai.harvester2.verb.HarvesterVerb;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import junit.framework.TestCase;
import org.w3c.dom.NodeList;

/**
 *
 * @author dennis
 */
@Entity
public class TestOaiPmhIdentify extends TestCase {
  String urlLocal = "http://localhost:8080/harvester/?verb=Identify";
  String url = "http://ir.ub.rug.nl/oai/";
  
  public void testIdentify() throws IOException, ParserConfigurationException, ResponseParsingException, TransformerException {
      HarvesterVerb verb = new Identify(url, Proxy.NO_PROXY, null);
      
      NodeList nodes = verb.getNodeList("/");
      
  }
}
