package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class RecordDOMImpl extends RecordImpl implements RecordDOM {
  private String pz2Namespace = "http://www.indexdata.com/pazpar2";
  private String pzRecord = "pz:record";
  private String pzMetadata = "pz:metadata";

  public RecordDOMImpl(Record record) {
    super(record.getValues());
    setId(record.getId());
    setDatabase(record.getDatabase());
  }

  public RecordDOMImpl(String id, String database, Node node) {
    setId(id);
    setDatabase(database);
    setNode(node);
  }

  public void setNode(Node node) {
    getValues().clear();
    merge(node);
  }

  /**
   * Merge a XML node into a record
   * @param node
   */
  public void merge(Node node) {
    if (Node.DOCUMENT_NODE == node.getNodeType()) {
      node = node.getFirstChild();
    }
    if (node.getLocalName().equals("record")) {
      NodeList nodes = node.getChildNodes();
      for (int index = 0; index < nodes.getLength(); index++) {
	serializeNode(nodes.item(index));
      }
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void serializeNode(Node node) {
    if (node.getLocalName().equals("metadata")) {
      Node typeAttribute = node.getAttributes().getNamedItem("type");
      String key = typeAttribute.getNodeValue();
      if (key != null)
      {
	Collection<Serializable> values = valueMap.get(key);
	if (values == null) { 
	  values = new LinkedList();
	  valueMap.put(key, values);
	}
	StringBuffer buffer = new StringBuffer();
	NodeList textNodes = node.getChildNodes();
	for (int index = 0; index < textNodes.getLength(); index++) {
	  String value = textNodes.item(index).getNodeValue();
	  buffer.append(value);
	}
	values.add(buffer.toString());
      }
    }
  }

  public Node toNode() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException("DOM Parser Configuration Error", e);
    }
    Document doc = builder.newDocument();
    Element recordElement = doc.createElementNS(pz2Namespace, pzRecord);
    doc.appendChild(recordElement);
    
    Map<String, Collection<Serializable>> keyValues = getValues();
    for (Object obj : keyValues.keySet()) {
      	if (obj instanceof String) {
      	  String key = (String) obj;
      	  Collection<Serializable> values = keyValues.get(key);
      	  for (Serializable value : values) {
      	    Element metadata = doc.createElementNS(pz2Namespace, pzMetadata);
      	    metadata.setAttribute("type", key);
      	    // TODO handle second level (holdings)
      	    Text valueElement = doc.createTextNode(value.toString());
      	    metadata.appendChild(valueElement);
      	    recordElement.appendChild(metadata);
      	  }
      	}
    }
    return doc;
  }

}
