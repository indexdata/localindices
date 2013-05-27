/*
 * Copyright (c) 1995-2013, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.integrationtests;

import com.indexdata.utils.XmlUtils;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author jakub
 */
public class IntTestUtil {
  public final static String HOST = "localhost";
  //WS is trailing slash sensitive
  public final static String PATH = "harvester/";
  public final static int PORT = Integer.parseInt(System.getProperty("jetty.port"));
  public final static String ROOT_URI = "http://"+HOST+":"+PORT+"/"+PATH;
  
  public static class TestException extends Exception {
    public TestException(String message, Throwable cause) {
      super(message, cause);
    }

    public TestException(String message) {
      super(message);
    }

    public TestException(Throwable cause) {
      super(cause);
    }
  }

  public static Document GET(String url) throws TestException {
    HttpClient hc = new HttpClient();
    //retrieve
    try {
      URI connUri = new URI(url);
      if (connUri.getUserInfo() != null && !connUri.getUserInfo().isEmpty()) {
        hc.getState().setCredentials(AuthScope.ANY,
          new UsernamePasswordCredentials(connUri.getUserInfo()));
      }
    } catch (URISyntaxException ue) {
      throw new TestException(ue);
    }
    HttpMethod hm = new GetMethod(url);
    try {
      int res = hc.executeMethod(hm);
      if (res != 200) {
        throw new TestException(res + " returned when connecting to " + url);
      }
      return XmlUtils.parse(hm.getResponseBodyAsStream());
    } catch (IOException ioe) {
      throw new TestException("IO error when connecting to " + url
        + ", aborting.", ioe);
    } catch (SAXException saxe) {
      throw new TestException("XML parsing error from " + url + ", aborting.",
        saxe);
    } finally {
      hm.releaseConnection();
    }
  }
}
