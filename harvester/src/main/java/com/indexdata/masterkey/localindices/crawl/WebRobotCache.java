/*
 * Caching for the robots.txt
 */
package com.indexdata.masterkey.localindices.crawl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Simple cache to keep the robots.txt files from various sites
 * 
 * @author heikki
 */
public class WebRobotCache {
  private static Logger logger = Logger
      .getLogger("com.indexdata.masterkey.localindices.crawl");
  private Map<URL, String> cache = new HashMap<URL, String>();
  private Proxy proxy;

  public WebRobotCache(Proxy proxy) {
    this.proxy = proxy;
  }

  private synchronized String getRobots(URL robUrl) 
  {
    String robtxt = cache.get(robUrl);
    // yes, we need to check here too, because some other thread might
    // have been fetching the very same file, and we may have been
    // waiting until it did the job.
    if (robtxt == null) {
      HTMLPage robpg;
      try {
	robpg = new HTMLPage(robUrl, proxy);
      } catch (IOException ex) {
	cache.put(robUrl, "");
	return ""; // no robots.txt, ok to go
	// FIXME - Is this the right way to handle all excpetions?
      }
      robtxt = robpg.getContent();
      cache.put(robUrl, robtxt);
      logger.log(Level.DEBUG,
	  "Got " + robUrl.toString() + " (" + robtxt.length() + " b)");
    }
    return robtxt;
  }

  public boolean checkRobots(URL url) {
    String strHost = url.getHost();
    if (strHost == null || strHost.isEmpty()) {
      // Should not happen!
      logger.log(Level.DEBUG, "Could not extract host from '" + url.toString()
	  + "' - skipping robots txt");
      return false;
    }
    String strRobot = "http://" + strHost + "/robots.txt";
    URL robUrl;
    try {
      robUrl = new URL(strRobot);
    } catch (MalformedURLException e) {
      // something weird is happening, so don't trust it
      logger.log(Level.DEBUG, "Could not create robot url " + "'" + strRobot
	  + "'");
      return false;
    }

    String robtxt = cache.get(robUrl);
    if (robtxt == null) {
      robtxt = this.getRobots(robUrl);
    }
    if (robtxt.isEmpty()) {
      return true; // no robots.txt, go ahead
      // Simplified, we assume all User-agent lines apply to us
      // Most likely nobody has (yet?) written a robots.txt section
      // for specifically for us.
    }
    Pattern p = Pattern.compile("^Disallow:\\s*(.*\\S)\\s*$",
	Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    Matcher m = p.matcher(robtxt);
    String urlpath = url.getPath();
    while (m.find()) {
      String path = m.group(1);
      if (urlpath.startsWith(path)) {
	logger.log(Level.TRACE, "Path '" + urlpath + "' forbidden "
	    + "by robot '" + path + "'");
	return false; // found one they don't want us to go to
      }
    }
    // logger.log(Level.TRACE, "Path '"+urlpath+"' all right ");
    return true;
  }
}
