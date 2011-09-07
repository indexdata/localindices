/*
 * One harvested web page
 * 
 *  Copyright (c) 1995-2008, Index Data
 *  All rights reserved.
 *  See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.crawl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * 
 * @author Heikki and Jakub
 */
public class HTMLPage {
  public final static int READ_BLOCK_SIZE = 1000000; // bytes to read in one op
  public final static int MAX_READ_SIZE = 10000000; // 10MB
  public final static int CONN_TIMEOUT = 30000; // ms to make a connection
  public final static int READ_TIMEOUT = 30000; // ms to read a block
  public final static String USER_AGENT_STRING = "IndexData Masterkey Web crawler";
  private URL url;
  private String error = "";
  private String contentType;
  @SuppressWarnings("unused")
  private int contentLength;
  private String content = ""; // the whole thing
  private List<URL> links = new Vector<URL>();
  private String plainText = "";
  private String title = "";
  private static Logger logger = Logger
      .getLogger("com.indexdata.masterkey.localindices.crawl");
  private String description;
  private String keywords;
  private String author;

  // Create a trust manager that does not validate certificate chains
  // This code found floating around on the net, for example at
  // http://www.exampledepot.com/egs/javax.net.ssl/TrustAll.html
  // It royally messes up most of the security implications of using
  // https, but for a crawler, we don't really care!
  private void DisableCertValidation() {
    TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	return null;
      }

      public void checkClientTrusted(
	  java.security.cert.X509Certificate[] certs, String authType) {
      }

      public void checkServerTrusted(
	  java.security.cert.X509Certificate[] certs, String authType) {
      }
    } };

    // Install the all-trusting trust manager
    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    } catch (Exception e) {
      logger.log(Level.ERROR,
	  "Installing trust-all manager failed. " + e.getMessage());
    }
    // Now you can access an https URL without having the certificate in the
    // truststore
  }

  public HTMLPage(URL url, Proxy proxy) throws IOException {
    this.url = url;
    try {
      read(request(proxy));
      parse();
    } catch (IOException ioe) {
      this.error = ioe.getMessage();
      logger.log(Level.DEBUG, "I/O error :" + this.error);
      throw ioe;
    }
  }

  public HTMLPage(InputStream is, URL url) throws IOException {
    this.url = url;
    read(is);
    parse();
  }

  public HTMLPage(String content, URL url) {
    this.url = url;
    this.content = content;
    parse();
  }

  public String getContent() {
    return content;
  }

  public String getContentType() {
    return contentType;
  }

  public String getError() {
    return error;
  }

  public String getTitle() {
    return title;
  }

  public URL getUrl() {
    return url;
  }

  public List<URL> getLinks() {
    return links;
  }

  private InputStream request(Proxy proxy) throws IOException {
    HttpURLConnection conn = null;
    if (url.getProtocol().equalsIgnoreCase("http")
	|| url.getProtocol().equalsIgnoreCase("https")) {
      logger.log(Level.TRACE, "Opening connection to " + url.toString());
    } else {
      throw new IOException("Only HTTP or HTTPS supported " + "(not "
	  + url.getProtocol() + ") at " + url.toString());
    }
    DisableCertValidation();
    HttpURLConnection.setFollowRedirects(true);
    HttpsURLConnection.setFollowRedirects(true);
    if (proxy != null)
      conn = (HttpURLConnection) url.openConnection(proxy);
    else
      conn = (HttpURLConnection) url.openConnection();
    conn.setAllowUserInteraction(false);
    conn.setRequestProperty("User-agent", USER_AGENT_STRING);
    conn.setConnectTimeout(CONN_TIMEOUT);
    conn.setReadTimeout(READ_TIMEOUT);
    InputStream urlStream = conn.getInputStream();
    int responseCode = conn.getResponseCode();
    int conteLength = conn.getContentLength();
    String contType = conn.getContentType();
    // Normally, the UrlConnection will follow redirections, but not to
    // https!!##
    if (responseCode == 302 && url.getProtocol().equalsIgnoreCase("http")) {
      logger.log(Level.TRACE,
	  "Got a 302 to l=" + conn.getHeaderField("Location"));
      String location = conn.getHeaderField("Location");
      url = new URL(location);
      if (url.getProtocol().equalsIgnoreCase("https")) {
	return request(proxy);
      }
    }
    // only OK
    if (responseCode != 200) {
      throw new IOException("Connection failed " + "(" + responseCode + ") at "
	  + url.toString());
      // Fixme - this requests the page once! And with 'Java' in user'agent
      // and below we fetch it once more! (with proper user-agent)
      // this is not needed - it's done anyways
      /*
       * if (contType == null || contType.isEmpty()) { contType =
       * URLConnection.guessContentTypeFromStream(urlStream); }
       */
    }
    if (contType == null || contType.isEmpty()) {
      throw new IOException("Could not verify content type at "
	  + url.toString());
    }
    if (!contType.startsWith("text/html") && !contType.startsWith("text/plain")) // Get
										 // also
										 // plain
										 // text,
										 // we
										 // need
										 // it
										 // for
										 // robots.txt,
										 // and
    // might as well index it all anyway
    {
      throw new IOException("Content type '" + contType
	  + "' not acceptable at " + url.toString());
    }
    this.url = conn.getURL(); // This may have changed if we process redirects
    this.contentType = contType;
    this.contentLength = conteLength;
    this.url = conn.getURL();
    return urlStream;
  }

  /**
   * Split a html page. First extract body and headers, then fulltext and links
   */
  private void parse() {
    if (content == null) {
      return;
    }
    Long startTime = System.currentTimeMillis();
    // Split headers and body, if possible
    Pattern p = Pattern.compile("<head>(.*)</head>.*" + "<body[^>]*>(.*)",
	Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    Matcher m = p.matcher(content);
    String headers = null, body = null;
    if (m.find()) {
      headers = m.group(1);
      body = m.group(2);
    } else {
      headers = content;// many times the websites have only <head> see
			// http://www.enviroliteracy.org/subcategory.php/161.html
      body = content;// doesn't look like good html, try to extract links anyway
    }
    Long et1 = System.currentTimeMillis() - startTime;
    startTime = System.currentTimeMillis();
    // Extract a title
    p = Pattern.compile("<title>\\s*(.*?)\\s*</title>",
	Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    // The *? modifier should make it reluctant, so we get the firs title
    // only, if there are several
    m = p.matcher(headers);
    if (m.find() && m.group(1) != null && !m.group(1).isEmpty()) {
      title = m.group(1);
      // FIXME - truncate to a decent max
    } else {
      title = "???"; // FIXME - try to get the first H1 tag,
      // or first text line or something
    }
    Long et2 = System.currentTimeMillis() - startTime;
    startTime = System.currentTimeMillis();

    // get meta tags
    p = Pattern
	.compile(
	    "<meta[^>]+name=['\"]?([^>'\"# ]+)['\"# ]?\\s*content=['\"]?([^>'\"#]+)['\"# ]?[^>]*>",
	    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    m = p.matcher(headers);
    while (m.find()) {
      String metaName = m.group(1);
      String metaContent = m.group(2);
      if ("description".equalsIgnoreCase(metaName)) {
	description = metaContent;
      } else if ("keywords".equalsIgnoreCase(metaName)) {
	keywords = metaContent;
      } else if ("author".equalsIgnoreCase(metaName)) {
	author = metaContent;
      }
    }

    // extract full text strip tags like <tag>sdlfksd</tags>
    p = Pattern.compile(
	"<\\s*(script|style|object|canvas|applet)[^>]*>.*?</\\s*\\1\\s*>",
	Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    m = p.matcher(body);
    String rawtext = m.replaceAll("");

    // extract full text strip tags like <tag/>
    p = Pattern.compile("<\\s*(script|style|object|canvas|applet).*?/\\s*>",
	Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    m = p.matcher(rawtext);
    rawtext = m.replaceAll("");

    p = Pattern.compile("<[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
	| Pattern.DOTALL);
    m = p.matcher(rawtext);
    plainText = m.replaceAll("");
    Long et3 = System.currentTimeMillis() - startTime;
    startTime = System.currentTimeMillis();

    // extract links
    p = Pattern.compile("<a[^>]+href=['\"]?([^>'\"# ]+)['\"# ]?[^>]*>",
	Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    m = p.matcher(body);
    while (m.find()) {
      String lnk = m.group(1);
      URL linkUrl = null;
      if (!lnk.startsWith("javascript:") && !lnk.startsWith("mailto:")) {
	try {
	  linkUrl = new URL(url, lnk.replaceAll("&amp;", "&"));
	  // For some reason we get some of the '&'s doubly encoded.
	  // As "&amp;" is not good in an URL anyway, we decode them here
	  // logger.log(Level.TRACE, "Made link " + linkUrl.toString() +
	  // " out of " + url.toString() + " and " + lnk );

	} catch (MalformedURLException ex) {
	  logger.log(Level.TRACE, "Could not make a good url from " + "'" + lnk
	      + "' " + "when parsing " + this.url.toString());
	}
	// if (linkUrl!= null && !this.links.contains(linkUrl)) {
	// For some reason, the links.contains test was awfully slow
	// - up to a minute for a list of 100 links. And with low CPU
	// load, it could not be just an inefficient implementation,
	// it must be some mysterious locking thing.
	// The solution for now is not to deduplicate the list here,
	// the crawler does its own deduplication anyway, and the bulk
	// upload should never have duplicates in the first place.
      }
      if (linkUrl != null) {
	this.links.add(linkUrl);
      }
    }
    Long et4 = System.currentTimeMillis() - startTime;
    startTime = System.currentTimeMillis();

    logger.log(Level.DEBUG, url + " title: '" + title + "' - " + links.size()
	+ " links");
    if (et1 + et2 + et3 + et4 > 1000) {
      logger.log(Level.DEBUG, "Parse timings: " + " body: " + et1 + " title: "
	  + et2 + " plain: " + et3 + " links: " + et4);
    }
  } // parse

  /*
   * Read in the entire input stream
   */
  private void read(InputStream is) throws IOException {
    byte[] b = new byte[READ_BLOCK_SIZE];
    int numRead = is.read(b);
    if (numRead <= 0) {
      content = "";
    } else {
      content = new String(b, 0, numRead);
      while ((numRead != -1) && (content.length() < MAX_READ_SIZE)) {
	numRead = is.read(b);
	if (numRead != -1) {
	  String newContent = new String(b, 0, numRead);
	  content += newContent;
	}
      }
    }
    is.close();
    logger.log(Level.TRACE, url.toString() + " Read " + content.length()
	+ " bytes.");
  } // read

  // this should be faster but does not appear so
  @SuppressWarnings("unused")
  private void read2(InputStream is) throws IOException {
    char[] b = new char[READ_BLOCK_SIZE];
    Reader r = new BufferedReader(new InputStreamReader(is));
    StringBuilder sb = new StringBuilder();
    while ((r.read(b)) != -1 && sb.length() < MAX_READ_SIZE) {
      sb.append(b);
    }
    r.close();
    content = sb.toString();
  } // read

  private String xmlTag(String tag, String data) {
    String clean = data.replaceAll("&", "&amp;"); // DIRTY - use proper XML
						  // tools
    clean = clean.replaceAll("<", "&lt;");
    clean = clean.replaceAll(">", "&gt;");
    clean = clean.replaceAll("\\s+", " ");
    clean = clean.replaceAll("\000", " ");
    clean = clean.replaceAll("\\p{Cntrl}", " ");
    return "<pz:metadata type=\"" + tag + "\">" + clean + "</pz:metadata>";
  }

  /** Convert the page into XML suitable for indexing with zebra */
  public String toPazpar2Metadata() {
    // FIXME - Use proper XML tools to do this, to avoid problems with
    // bad entities, character sets, etc.
    String xml = "<pz:record>\n";
    xml += xmlTag("title", title);
    if (description != null)
      xml += xmlTag("description", description);
    if (keywords != null) {
      for (String keyword : keywords.split(",")) {
	xml += xmlTag("subject", keyword);
      }
    }
    if (author != null) {
      xml += xmlTag("author", author);
    }
    xml += xmlTag("electronic-url", url.toString());
    xml += xmlTag("fulltext", plainText);
    xml += "</pz:record>\n";
    return xml;
  } // makeXml
}
