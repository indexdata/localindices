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
import java.net.URL;
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
    public final static int READ_BLOCK_SIZE = 1000000; // bytes to read in one op
    public final static int MAX_READ_SIZE = 10000000; // 10MB
    public final static int CONN_TIMEOUT = 30000; // ms to make a connection
    public final static int READ_TIMEOUT = 30000; // ms to read a block
    public final static String USER_AGENT_STRING = "IndexData Masterkey Web crawler";
    
    private URL url;
    private String error = "";
    private String contentType;
    private int contentLength;
    private String content = ""; // the whole thing
    private String headers = "";
    private String body = "";
    private List<URL> links = new Vector<URL>();
    private String plaintext = "";
    private String title = "";
    private static Logger logger =
            Logger.getLogger("com.indexdata.masterkey.localindices.crawl");

    public HTMLPage(URL url) throws IOException {
        this.url = url;
        try {
            read(request());
            parse();
        } catch (IOException ioe) {
            this.error = ioe.getMessage();
            logger.log(Level.ERROR, this.error);
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

    public String getBody() {
        return body;
    }

    public String getContenttype() {
        return contentType;
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

    private InputStream request( ) throws IOException {
        if (!url.getProtocol().equalsIgnoreCase("http"))
            throw new IOException("Only HTTP supported,");
        logger.log(Level.TRACE, "Opening connection to " + url.toString());
        HttpURLConnection.setFollowRedirects(true);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setAllowUserInteraction(false);
        conn.setRequestProperty("User-agent", USER_AGENT_STRING);
        conn.setConnectTimeout(CONN_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        InputStream urlStream = conn.getInputStream();
        int responseCode = conn.getResponseCode();
        int contentLength = conn.getContentLength();
        String contentType = conn.getContentType();
        // only OK
        if (responseCode != 200)
            throw new IOException("HTTP connection failed (" + responseCode + ") at " +
                    url.toString());
        // Fixme - this requests the page once! And with 'Java' in user'agent
        // and below we fetch it once more! (with proper user-agent)
        
        // this is not needed - it's done anyways
        /*if (contentType == null || contentType.isEmpty()) {
            contentType = URLConnection.guessContentTypeFromStream(urlStream);
        }
        */
        if (contentType == null || contentType.isEmpty())
            throw new IOException("Could not verify content type at " + url.toString());
        if (!contentType.startsWith("text/html") 
                && !contentType.startsWith("text/plain"))
            // Get also plain text, we need it for robots.txt, and
            // might as well index it all anyway
            throw new IOException("Content type '" + contentType + "' not acceptable at" + url.toString());
            
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.url = conn.getURL();
        return urlStream;
    }

    /** Split a html page. 
     * First extract body and headers, then fulltext and links 
     */
    private void parse() {
        if (content == null) return;
        // Split headers and body, if possible
        Pattern p = Pattern.compile("<head>(.*)</head>.*" +
                "<body[^>]*>(.*)",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m = p.matcher(content);
        if (m.find()) {
            //TODO the headers shoould be further spread apart, there's some md to get out
            headers = m.group(1);
            body = m.group(2);
        } else {
            headers = "";
            body = content; // doesn't look like good html, try to extract links anyway
        }
        // Extract a title
        //p = Pattern.compile("<title>\\s*(.*\\S)??\\s*</title>",
        p = Pattern.compile("<title>\\s*(.*)??</title>",
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
        //logger.log(Level.TRACE, "Plaintext: " + this.plaintext);

        // extract links
        p = Pattern.compile("<a[^>]+href=['\"]?([^>'\"#]+)['\"# ]?[^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        m = p.matcher(body);
        while (m.find()) {
            String lnk = m.group(1);
            URL linkUrl=null;
            try {
                linkUrl = new URL(url, lnk);
                //logger.log(Level.TRACE, "Made link " + linkUrl.toString() + 
                //        " out of " + url.toString() + " and " + lnk );
                        
            } catch (MalformedURLException ex) {
                logger.log(Level.TRACE, "Could not make a good url from " +
                        "'" + lnk + "' " +
                        "when parsing " + this.url.toString());
            }
            if (linkUrl!= null && !this.links.contains(linkUrl)) {
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
        logger.log(Level.TRACE, url.toString() + " Read " + content.length() + " bytes.");
    } //read
    
    //this should be faster but does not appear so
    private void read2(InputStream is) throws IOException {
        char[] b = new char[READ_BLOCK_SIZE];
        Reader r = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        int charsRead = -1;
        while ((charsRead = r.read(b)) != -1 && sb.length() < MAX_READ_SIZE) {
            sb.append(b);
        }
        r.close();
        content = sb.toString();
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
    public String toPazpar2Metadata() {
        // FIXME - Use proper XML tools to do this, to avoid problems with
        // bad entities, character sets, etc.
        String xml = "<pz:record>\n";
        xml += xmlTag("md-title", title);
        xml += xmlTag("md-electronic-url", url.toString());
        xml += xmlTag("md-fulltext", plaintext);
        xml += "</pz:record>\n";
        return xml;
    } // makeXml
}
