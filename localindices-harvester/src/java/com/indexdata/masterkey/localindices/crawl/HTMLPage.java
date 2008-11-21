/*
 * One harvested web page
 * 
 *  Copyright (c) 1995-2008, Index Data
 *  All rights reserved.
 *  See the file LICENSE for details.
 */
package com.indexdata.masterkey.localindices.crawl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author Heikki and Jakub
 */
public class HTMLPage {

    private URL url;
    private String error = "";
    private String contenttype = "";
    private String content = ""; // the whole thing
    private String headers = "";
    private String body = "";
    private List<URL> links = new Vector<URL>();
    private String plaintext = "";
    private String title = "";
    public String xml = ""; // FIXME - move XML stuff here too
    private static Logger logger =
            Logger.getLogger("com.indexdata.masterkey.localindices.crawl");
    public final static int readBlockSize = 1000000; // bytes to read in one op
    public final static int maxReadSize = 10000000; // 10MB
    public final static int connTimeOut = 30000; // ms to make a connection
    public final static int readTimeOut = 30000; // ms to read a block
    public final static String userAgentString = "IndexData Masterkey Web crawler";

    public HTMLPage(URL url) {
        this.url = url;
        try {
            read(request());
            parse();
        } catch (IOException ioe) {
            this.error = ioe.getMessage();
            logger.log(Level.ERROR, this.error);
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

    public String getBody() {
        return body;
    }

    public String getContenttype() {
        return contenttype;
    }

    public String getError() {
        return error;
    }

    public String getHeaders() {
        return headers;
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

    private InputStream request() throws IOException {
        content = "";
        String pageUrl = url.toString();
        logger.log(Level.TRACE, "About to fetch '" + pageUrl + "'");
        if (url.getProtocol().compareTo("http") != 0) {
            throw new IOException("Unsupported protocol in '" + pageUrl + "'");
        }
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(connTimeOut);
        conn.setReadTimeout(readTimeOut);
        conn.setAllowUserInteraction(false);
        conn.setRequestProperty("User-agent", userAgentString);
        InputStream urlStream = url.openStream();
        this.contenttype = conn.getContentType();
        // Fixme - this requests the page once! And with 'Java' in user'agent
        // and below we fetch it once more! (with proper user-agent)
        if (this.contenttype == null || this.contenttype.isEmpty()) {
            this.contenttype = URLConnection.guessContentTypeFromStream(urlStream);
        }
        if (this.contenttype == null || this.contenttype.isEmpty()) {
            throw new IOException("Could not verify content type at " + pageUrl);
        }
        if (!this.contenttype.startsWith("text/html") &&
                !this.contenttype.startsWith("text/plain")) {
            // Get also plain text, we need it for robots.txt, and
            // might as well index it all anyway
            throw new IOException("Content type '" + contenttype + "' not acceptable at" + pageUrl);
        }
        return urlStream;
    }

    /** Split a html page. 
     * First extract body and headers, then fulltext and links 
     */
    private void parse() {
        // Split headers and body, if possible
        Pattern p = Pattern.compile("<head>(.*)</head>.*" +
                "<body[^>]*>(.*)",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m = p.matcher(this.content);
        if (m.find()) {
            //logger.log(Level.TRACE, "P1 matched.");
            this.headers = m.group(1);
            this.body = m.group(2);
        } else {
            logger.log(Level.TRACE, "P1 did NOT match. p='" + p.pattern() + "'");
            this.headers = "";
            this.body = this.content; // doesn't look like good html, try to extract links anyway
        }
        // Extract a title
        this.title = "";
        p = Pattern.compile("<title>\\s*(.*\\S)??\\s*</title>",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        // The ?? modifier should make it reluctant, so we get the firs title
        // only, if there are several FIXME - does not work
        m = p.matcher(this.headers);
        if (m.find() && m.group(1) != null && !m.group(1).isEmpty()) {
            this.title = m.group(1);
        // FIXME - truncate to a decent max
        } else {
            this.title = "???"; // FIXME - try to get the first H1 tag, 
        // or first text line or something
        }

        // extract full text
        p = Pattern.compile("<[^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        m = p.matcher(this.content);
        this.plaintext = m.replaceAll("");
        //logger.log(Level.TRACE, "Plaintext: " + this.plaintext);

        // extract links
        p = Pattern.compile("<a[^>]+href=['\"]?([^>'\"#]+)['\"# ]?[^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        m = p.matcher(this.body);
        while (m.find()) {
            String lnk = m.group(1);
            URL linkUrl;
            try {
                linkUrl = new URL(this.url, lnk);
            } catch (MalformedURLException ex) {
                logger.log(Level.TRACE, "Could not make a good url from " +
                        "'" + lnk + "' " +
                        "when parsing " + this.url.toString());
                break;
            }
            if (!this.links.contains(linkUrl)) {
                this.links.add(linkUrl);
            }
        }

        logger.log(Level.DEBUG,
                this.url + " " +
                "title:'" + this.title + "' (" +
                "h=" + this.headers.length() + "b " +
                "b=" + this.body.length() + "b) " +
                this.links.size() + " links");
    } // parse

    /*
     * Read in the entire input stream
     */
    private void read(InputStream is) throws IOException {
        byte[] b = new byte[readBlockSize];
        int numRead = is.read(b);
        if (numRead <= 0) {
            content = "";
        } else {
            content = new String(b, 0, numRead);
            while ((numRead != -1) && (content.length() < maxReadSize)) {
                numRead = is.read(b);
                if (numRead != -1) {
                    String newContent = new String(b, 0, numRead);
                    content += newContent;
                }
            }
        }
        is.close();
        logger.log(Level.TRACE, url.toString() + " Read " + content.length() + " bytes.");
    } //read

    private String xmlTag(String tag, String data) {
        String clean = data.replaceAll("&", "&amp;"); // DIRTY - use proper XML tools
        clean = clean.replaceAll("<", "&lt;");
        clean = clean.replaceAll(">", "&gt;");
        clean = clean.replaceAll("\\s+", " ");
        return "<pz:metadata type=\"" + tag + "\">" +
                clean +
                "</pz:metadata>";
    }

    /** Convert the page into XML suitable for indexing with zebra */
    public String xmlFragment() {
        // FIXME - Use proper XML tools to do this, to avoid problems with
        // bad entities, character sets, etc.
        String xml = "<pz:record>\n";
        xml += xmlTag("md-title", title);
        xml += xmlTag("md-fulltext", plaintext);
        xml += xmlTag("md-electronic-url", url.toString());
        xml += "</pz:record>\n";
        return xml;
    } // makeXml
}
