package com.indexdata.masterkey.localindices.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.stream.Stream;

import javax.xml.transform.TransformerException;

import com.indexdata.utils.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class FailedRecords {
    final String logDir;
    final long jobId;
    final String directory;

    public FailedRecords (String logDir, long jobId) {
        this.logDir = logDir;
        this.jobId = jobId;
        this.directory = logDir + "/failed-records/" + jobId + "/";
    }

    public File[] getRecords() throws FileNotFoundException {
        File dir = new File(directory);
        if (!dir.isDirectory()) {
          throw new FileNotFoundException("Error file directory not found: "+directory);
        }
      return dir.listFiles();
    }

    public Iterator<File> getFilesIterator() throws FileNotFoundException {
        File[] files = getRecords();
        return Arrays.asList(files).iterator();
    }

    public String getListOfFailedRecordsAsXml(URI baseUri, String originalRecordPath) throws FileNotFoundException {
      Iterator<File> files = getFilesIterator();
      StringBuilder response = new StringBuilder();
      response.append("<failed-records count=\"").append(getRecords().length).append("\">");
      while (files.hasNext()) {
        File file = files.next();
        String url = baseUri.toString() + "harvestables/" + jobId + "/failed-records/" + file.getName();
        response
        .append("<record>")
         .append("<file>")
          .append("<name>")
           .append(file.getName())
          .append("</name>")
          .append("<date>")
           .append(new Date(file.lastModified()))
          .append("</date>")
         .append("</file>")
         .append("<url>")
           .append(url)
         .append("</url>");
          try {
            if (originalRecordPath != null && originalRecordPath.length()>0) {
              response
              .append("<original-record-url>")
                .append(url).append("?element=").append(URLEncoder.encode(originalRecordPath,"UTF-8"))
              .append("</original-record-url>");
            }
          } catch (UnsupportedEncodingException uee) {}
          response.append("</record>");
        }
      response.append("</failed-records>");
      return response.toString();
    }

    public String getFailedRecordAsString (String name, String pathToElement) throws IOException {
      String failedRecord;
      StringBuilder contentBuilder = new StringBuilder();
      try (Stream<String> stream = Files.lines( Paths.get(directory, name), StandardCharsets.UTF_8)) {
        stream.forEach(s -> contentBuilder.append(s).append(System.lineSeparator()));
      }
      if (pathToElement != null && pathToElement.length()>0) {
        failedRecord = getRecordFragment (contentBuilder.toString(), pathToElement);
      } else {
        failedRecord = contentBuilder.toString();
      }
      return failedRecord;
    }

    /**
     * Extracts specified child element of the failed-record, based on the provided "path"
     *
     * @param failedRecordXml the failed-record XML
     * @param pathToElement a comma separated list of elements leading to the desired element to extract
     * @return the child element pointed to by pathToElement or the entire record if the pathToElement
     * did not resolve to any Element
     */
    private String getRecordFragment (String failedRecordXml, String pathToElement) {
      String[] legs = pathToElement.split(",");
      Document failedRecord;
      String fragmentString = "";
      StringReader reader = new StringReader(failedRecordXml);
      try {
        failedRecord = XmlUtils.parse(reader);
      } catch (SAXException | IOException e) {
        System.out.println("Failed to create XML document from failed record XML string: " + e.getMessage());
        return failedRecordXml;
      }
      Element documentElement = failedRecord.getDocumentElement();
      Element fragmentElement = getElement(documentElement, legs);
      StringWriter writer = new StringWriter();
      try {
        XmlUtils.serialize(fragmentElement, writer);
        fragmentString = writer.toString();
      } catch (TransformerException e) {
        System.out.println("Failed to write original record element to string " + e.getMessage());
        return failedRecordXml;
      }
      return fragmentString;
    }

    /**
     * Finds a child element by traversing document tree using a comma separated "path" string
     *
     * It will only consider Nodes that are Elements and for a repeated Element it will pick
     * the first occurrence.
     *
     * @param root the starting point
     * @param path list of Element localNames pointing down into a hierarchical structure of elements
     */
    private Element getElement (Element root, String ... path) {
      Element found = null;
      NodeList nodes = root.getChildNodes();
      for (String leg : path) {
        found = null;
        for (int i=0; i <nodes.getLength(); i++) {
          Node node = nodes.item(i);
          if (node.getNodeType() == Node.ELEMENT_NODE) {
            if (node.getLocalName().equals(leg)) {
              found = (Element) node;
              break;
            }
          }
        }
        if (found == null) {
          break;
        } else {
          nodes = found.getChildNodes();
        }
      }
      return found;
    }

}