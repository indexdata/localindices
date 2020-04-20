package com.indexdata.masterkey.localindices.util;

import java.io.IOException;
import java.io.StringReader;

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
    String namespace = "http://www.loc.gov/MARC21/slim";
    Element record = null;
    if(root.getTagName().endsWith("OAI-PMH")) { // probably a static OAI-PMH file
      Element listRecords = (Element)root.getElementsByTagNameNS(namespace, "ListRecords").item(0);
      Element topRecord = (Element)listRecords.getElementsByTagNameNS(namespace, "record").item(0);
      Element metadata = (Element)topRecord.getElementsByTagNameNS(namespace, "metadata").item(0);
      record = (Element) metadata.getElementsByTagNameNS(namespace, "record").item(0);
    } else if (root.getTagName().endsWith("record")) {
      //record = (Element) root.getElementsByTagName("record").item(0);
      record = root;
    } else if (root.getTagName().endsWith("collection")) {
      record = (Element) root.getElementsByTagNameNS(namespace, "record").item(0);
    }
    if(record == null) {
      throw new IOException("No record element found for root element " + root.getTagName());
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
      if(childElement.getTagName().endsWith("leader")) {
        marcJson.put("leader", textContent);
      } else if(childElement.getTagName().endsWith("controlfield")) {
        JSONObject field = new JSONObject();
        String marcTag = childElement.getAttribute("tag");
        field.put(marcTag, textContent);
        fields.add(field);
      } else if(childElement.getTagName().endsWith("datafield")) {
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
        NodeList nodeList = childElement.getElementsByTagNameNS(namespace, "subfield");
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

}
