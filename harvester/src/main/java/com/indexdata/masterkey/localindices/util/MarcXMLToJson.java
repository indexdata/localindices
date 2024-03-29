/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.indexdata.masterkey.localindices.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;

import java.util.Scanner;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class MarcXMLToJson {

  public static JSONObject convertMarcXMLToJson(String marcXML)
      throws SAXException, IOException, ParserConfigurationException {
    JSONObject marcJson = new JSONObject();
    JSONArray fields = new JSONArray();
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    Document document = documentBuilder.parse(new InputSource(new StringReader(marcXML)));
    Element root = document.getDocumentElement();
    Element record = null;
    if(root.getTagName().equals("OAI-PMH")) { // probably a static OAI-PMH file
      Element listRecords = (Element)root.getElementsByTagName("ListRecords").item(0);
      Element topRecord = (Element)listRecords.getElementsByTagName("record").item(0);
      Element metadata = (Element)topRecord.getElementsByTagName("metadata").item(0);
      record = (Element) metadata.getElementsByTagName("record").item(0);
    } else if (root.getTagName().equals("record")) {
      NodeList recordsEmbeddedInRecord = root.getElementsByTagNameNS("*","record");
      if (recordsEmbeddedInRecord != null && recordsEmbeddedInRecord.getLength()==1) {
        // e.g. a MARC record embedded in OAI-PMH record
        record = (Element) recordsEmbeddedInRecord.item(0);
      } else {
        record = root;
      }
    } else if (root.getLocalName().equals("collection")) {
      NodeList records = root.getElementsByTagNameNS("*", "record");
      if (records != null && records.getLength()==1) {
        record = (Element) records.item(0);
      }
      if (records != null && records.getLength()>1) {
        throw new IOException("MARC to XML converter received collection with multiple records. Cannot process multiple.");  
      }
    }
    if(record == null) {
      throw new IOException("No record element found");
    }
    Node childNode = record.getFirstChild();
    Element childElement;
    while(childNode != null) {
      if(childNode.getNodeType() != record.getNodeType())
      {
        childNode = childNode.getNextSibling();
        continue;
      }
      childElement = (Element)childNode;
      String textContent = childElement.getTextContent();
      if(childElement.getLocalName().equals("leader")) {
        marcJson.put("leader", textContent);
      } else if(childElement.getLocalName().equals("controlfield")) {
        JSONObject field = new JSONObject();
        String marcTag = childElement.getAttribute("tag");
        field.put(marcTag, textContent);
        fields.add(field);
      } else if(childElement.getLocalName().equals("datafield")) {
        JSONObject field = new JSONObject();
        JSONObject fieldContent = new JSONObject();
        String marcTag = childElement.getAttribute("tag");
        if(childElement.hasAttribute("ind1")) {
          fieldContent.put("ind1", childElement.getAttribute("ind1"));
        }
        if(childElement.hasAttribute("ind1")) {
          fieldContent.put("ind2", childElement.getAttribute("ind2"));
        }
        JSONArray subfields = new JSONArray();
        fieldContent.put("subfields", subfields);
        NodeList nodeList = childElement.getElementsByTagNameNS( "*", "subfield");
        for(int i = 0; i < nodeList.getLength(); i++) {
          Element subField = (Element) nodeList.item(i);
          String code = subField.getAttribute("code");
          String content = subField.getTextContent();
          JSONObject subfieldJson = new JSONObject();
          subfieldJson.put(code, content);
          subfields.add(subfieldJson);
        }
        field.put(marcTag, fieldContent);
        fields.add(field);
      }
      childNode = childNode.getNextSibling();
    }
    if(fields.size() > 0) {
      marcJson.put("fields", fields);
    }

    return marcJson;
  }

  /**
   * Test method, pass full path to the MARC XML file to parse as the argument to main.
   */
  public static void main (String[] args) {
    File marcRecord = new File(args[0]);
    try {
      Scanner sc = new Scanner(marcRecord);
      sc.useDelimiter("\\Z");
      try {
        JSONObject parsedMarc = convertMarcXMLToJson(sc.next());
        System.out.println("Parsed MARC:");
        System.out.println(parsedMarc.toJSONString());
      } catch (SAXException | IOException | ParserConfigurationException e) {
        e.printStackTrace();
      }
    } catch (FileNotFoundException fnfe) {
      fnfe.printStackTrace();
    }
  }

}
