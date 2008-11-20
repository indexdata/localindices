/*
 * One harvested web page
 */
package com.indexdata.masterkey.localindices.harvest.job;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author heikki
 */
public class WebPage {
    private static Logger logger = Logger.getLogger("com.indexdata.masterkey.harvester");
    private final static int connTimeOut = 30000; // ms to make a connection
    private final static int readTimeOut = 30000; // ms to read a block
    private final static int readBlockSize = 1000000; // bytes to read in one op
    private final static int maxReadSize = 10000000; // 10MB 
    private final static String userAgentString = "IndexData Masterkey Web crawler robot";

    
    // FIXME - make private, and a get functions where needed. 
    public URL url = null;
    public String error = "";
    public String contenttype = "";
    public String content = ""; // the whole thing
    public String headers = "";
    public String body = "";
    public Vector<URL> links = new Vector<URL>();
    public String plaintext = "";
    public String title = "";
    public String xml = "";

    private String trunc (String s, int len) {
        if (s.length()<=len)
            return s;
        return s.substring(0,len-1);
    }

    
    /** Constructor that fetches the page */
    public WebPage(URL url) {
        this.url = url;
        this.fetchPage();
    }

    /** Fetch the web page. */
    private void fetchPage() {
        error = null;
        content = "";
        String pageUrl = url.toString();
        logger.log(Level.TRACE, "About to fetch '" + pageUrl + "'");
        if (url.getProtocol().compareTo("http") != 0) {
            error = "Unsupported protocol in '" + pageUrl + "'";
            logger.log(Level.ERROR, error);
            return;
        }
        try {
            URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(connTimeOut);
            urlConnection.setReadTimeout(readTimeOut);
            urlConnection.setAllowUserInteraction(false);
            urlConnection.setRequestProperty("User-agent", userAgentString);
            InputStream urlStream = url.openStream();
            contenttype = urlConnection.getContentType();
            // Fixme - this requests the page once! And with 'Java' in user'agent
            // and below we fetch it once more! (with proper user-agent)
            if (contenttype == null || contenttype.isEmpty()) {
                contenttype = URLConnection.guessContentTypeFromStream(urlStream);
            }
            if (contenttype == null || contenttype.isEmpty()) {
                error = "Skipped '" + pageUrl + "'. could not get content type";
                logger.log(Level.DEBUG, error);
                return;
            }
            if (!contenttype.startsWith("text/html") &&
                    !contenttype.startsWith("text/plain")) {
                // Get also plain text, we need it for robots.txt, and
                // might as well index it all anyway
                error = "Skipped '" + pageUrl + "'. Content type '" +
                        contenttype + "' not acceptable ";
                logger.log(Level.DEBUG, error);
                return;
            }
            // search the input stream for links
            // first, read in the entire URL
            byte b[] = new byte[readBlockSize];
            int numRead = urlStream.read(b);
            if (numRead <= 0) {
                content = "";
            } else {
                content = new String(b, 0, numRead);
                while ((numRead != -1) && (content.length() < maxReadSize)) {
                    numRead = urlStream.read(b);
                    if (numRead != -1) {
                        String newContent = new String(b, 0, numRead);
                        content += newContent;
                    }
                }
            }
            urlStream.close();
            logger.log(Level.TRACE, pageUrl + " Read " + content.length() + " bytes");
            //logger.log(Level.DEBUG, content );
        } catch (FileNotFoundException ex) {
            error = "I/O Exception: " + pageUrl + " Not found ";
        // FIXME - Display also the referring page
        } catch (IOException ex) {
            error = "I/O Exception " +
                    "(" + ex.getClass().getSimpleName() + ") " +
                    "with " + pageUrl + ": " + ex.getMessage();
            logger.log(Level.ERROR,
                    "I/O Exception " +
                    "(" + ex.getClass().getSimpleName() + ") " +
                    "with " + pageUrl + ": " + ex.getMessage());
        }
        logger.log(Level.DEBUG, error);
    } // fetchPage
    /** Split a html page. 
     * First extract body and headers, then fulltext and links 
     */
    public void splitHtmlPage() {
        // Split headers and body, if possible
        Pattern p = Pattern.compile("<head>(.*)</head>.*" +
                "<body[^>]*>(.*)",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m = p.matcher(content);
        if (m.find()) {
            //logger.log(Level.TRACE, "P1 matched.");
            headers = m.group(1);
            body = m.group(2);
        } else {
            logger.log(Level.TRACE, "P1 did NOT match. p='" + p.pattern() + "'");
            headers = "";
            body = content; // doesn't look like good html, try to extract links anyway
        }
        // Extract a title
        title = "";
        p = Pattern.compile("<title>\\s*(.*\\S)??\\s*</title>",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
                // The ?? modifier should make it reluctant, so we get the firs title
                // only, if there are several FIXME - does not work
        m = p.matcher(headers);
        if (m.find() && m.group(1) != null && !m.group(1).isEmpty()) {
            title = m.group(1);
            // FIXME - truncate to a decent max
        } else {
            title = "???"; // FIXME - try to get the first H1 tag, 
        // or first text line or something
        }

        // extract full text
        p = Pattern.compile("<[^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        m = p.matcher(content);
        plaintext = m.replaceAll("");
        //logger.log(Level.TRACE, "Plaintext: " + plaintext);

        // extract links
        p = Pattern.compile("<a[^>]+href=['\"]?([^>'\"#]+)['\"# ]?[^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        m = p.matcher(body);
        while (m.find()) {
            String lnk = m.group(1);
            URL linkUrl;
            try {
                linkUrl = new URL(url, lnk);
            } catch (MalformedURLException ex) {
                logger.log(Level.TRACE, "Could not make a good url from " +
                        "'" + lnk + "' " +
                        "when parsing " + url.toString());
                break;
            }
            //logger.log(Level.TRACE, "Found link '" + m3.group() + "' '" + lnk + "' " +
            //        "-> '" + linkUrl.toString() + "'");
            if (!links.contains(linkUrl)) {
                links.add(linkUrl);
            }
        }
/*
        logger.log(Level.DEBUG,
                "JOB#" + resource.getId() + " " +
                round + ":" + searched.size() + "/" + numtosearch + " " +
                url + " " +
                "title:'" + title + "' (" +
                "h=" + headers.length() + "b " +
                "b=" + body.length() + "b) " +
                links.size() + " links");
 */
    } // splitHtmlPage

    /** Split a plain-text link page. Only gets the links
     */
    public void splitTextLinkPage() {
        links.clear();
        //Pattern p = Pattern.compile("(http://\\S+)",
        Pattern p = Pattern.compile("(http://[^ <>]+)",
                Pattern.CASE_INSENSITIVE );
        Matcher m = p.matcher(body);
        logger.log(Level.TRACE, "Parsing text links from " +
                body.length() + "bytes " + trunc(body,50) );
        while (m.find()) {
            String lnk = m.group(1);
            URL linkUrl;
            try {
                linkUrl = new URL(url, lnk);
            } catch (MalformedURLException ex) {
                logger.log(Level.TRACE, "Could not make a good url from " +
                        "'" + lnk + "' " +
                        "when parsing " + url.toString());
                break;
            }
            logger.log(Level.TRACE, "Found link '" + m.group(1) + "' '" + lnk + "' " +
                    "-> '" + linkUrl.toString() + "'");
            if (!links.contains(linkUrl)) {
                links.add(linkUrl);
            }
        }
    }

}
