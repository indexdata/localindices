package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ORG.oclc.oai.harvester2.verb.XPathHelper;

public class RecordDOMImpl extends RecordImpl implements RecordDOM {
  private String pz2Namespace = "http://www.indexdata.com/pazpar2/1.0";
  private String pzPrefix = "pz";
  private String recordField = "record";
  private String pzRecord = pzPrefix + ":" + recordField;
  private String metadataField = "metadata";
  private String pzMetadata = pzPrefix + ":" + metadataField;
  private String typeName = "type";
  @SuppressWarnings("unused")
  private String pzType = pzPrefix + ":" + typeName;
  private Node node = null;
  private NamespaceContext pzNsContext = new PzNamespaceContext();
  private NamespaceContext oaiNsContext = new OaiNamespaceContext();
  private String xpathNodes = ".//pz:metadata";
  private String xpathStatusPz = ".//pz:metadata[@type='status']";
  private String xpathStatusOai = ".//oai20:header/@status";
  private String xpathId = ".//pz:metadata[@type='id']";
  Logger logger = Logger.getLogger("com.indexdata.masterkey.localindices");

  public RecordDOMImpl(Record record) {
    if (record instanceof RecordDOM)
      setNode(((RecordDOMImpl) record).toNode());
    else
      valueMap = record.getValues();
    setId(record.getId());
    setDatabase(record.getDatabase());
    setDeleted(record.isDeleted());
    setOriginalContent(record.getOriginalContent());
  }

  public RecordDOMImpl(RecordDOMImpl record, NamespaceContext context) {
    setNode(record.node);
    setId(record.getId());
    setDatabase(record.getDatabase());
    setDeleted(record.isDeleted());
    setOriginalContent(record.getOriginalContent());
  }

  public RecordDOMImpl(String id, String database, Node node, byte[] originalContent) {
    setId(id);
    setDatabase(database);
    setNode(node);
    setOriginalContent(originalContent);
  }

  public void setNode(Node node) {
    this.node = node;
    extractDelete();
    extractId();
  }

  protected void extractDelete() {
    XPathHelper<String> xpathHelperDeletePz = new XPathHelper<String>(XPathConstants.STRING, pzNsContext);
    XPathHelper<String> xpathHelperDeleteOai = new XPathHelper<String>(XPathConstants.STRING, oaiNsContext);
    String delete;
    try {
      delete = xpathHelperDeletePz.evaluate(node, xpathStatusPz);
      if (delete != null && "deleted".equalsIgnoreCase(delete)) {
         this.isDeleted = true;
        } else {
          delete = new XPathHelper<String>(XPathConstants.STRING).evaluate(node, ".//record/status");
          if (delete != null && "deleted".equalsIgnoreCase(delete)) {
            this.isDeleted = true;
          } else {
            delete = xpathHelperDeleteOai.evaluate(node, xpathStatusOai);
            if (delete != null && "deleted".equalsIgnoreCase(delete)) {
              this.isDeleted = true;
            }
          }
        }

    } catch (XPathExpressionException e) {
      e.printStackTrace();
    }
  }

  protected void extractId()
  {
    XPathHelper<String> xpathHelperDelete = new XPathHelper<String>(String.class, pzNsContext);
    try {
      if (id == null) {
	String newid = xpathHelperDelete.evaluate(node, xpathId);
	if (!"".equals(newid))
	  this.id = newid;
      }
    } catch (XPathExpressionException e) {
      e.printStackTrace();
    }
  }

  @Override
  public boolean isCollection() {
    if (node != null) {
      Element root;
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        root = (Element) node;
      } else if (node.getNodeType() == Node.DOCUMENT_NODE) {
        root = ((Document) node).getDocumentElement();
      } else {
        root = node.getOwnerDocument().getDocumentElement();
      }
      boolean isCollection = (
              root != null
              && (root.getTagName().equals("collection") ||
                  root.getTagName().equals("pz:collection"))
              );
      return isCollection;
    } else {
      return false;
    }
  }

  public Collection<Record> getSubRecords() {
    if (!isCollection()) return null;
    NodeList children = node.getNodeType() == Node.DOCUMENT_NODE
      ? ((Document) node).getDocumentElement().getChildNodes()
      : node.getChildNodes();
    List<Record> list = new ArrayList<Record>(children.getLength());
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() != Node.ELEMENT_NODE) continue;
      Element childElem = (Element) child;
      //original record is set to null since at this point the collection
      //is most likely transformed and we can't "extract' matching original rec
      //instead, to store original records, the input should be 'split at depth'
      list.add(new RecordDOMImpl(null, null, child, null));
    }
    return list;
  }

  public Map<String, Collection<Serializable>> getValues() {
    if (node == null)
      return valueMap;
    valueMap.clear();
    try {
      XPathHelper<NodeList> xpathHelper = new XPathHelper<NodeList>(XPathConstants.NODESET, pzNsContext);
      NodeList nodeList = xpathHelper.evaluate(node, xpathNodes);
      for (int index = 0; index < nodeList.getLength(); index++) {
        Node md = nodeList.item(index);
	serializeNode(nodeList.item(index));
      }
    } catch (XPathExpressionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return valueMap;
  }

  /**
   * Merge a XML node into a record
   * @param node
   *
   * TODO Only works on PZ nodes
   */
  public void merge(Node node) {
    if (Node.DOCUMENT_NODE == node.getNodeType()) {
      node = node.getFirstChild();
    }
    if (node.getLocalName().equals(recordField)) {
      NodeList nodes = node.getChildNodes();
      for (int index = 0; index < nodes.getLength(); index++) {
	serializeNode(nodes.item(index));
      }
    }
    extractDelete();
    extractId();
  }

  // TODO Only works on PZ nodes
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void serializeNode(Node node) {
    if (node.getLocalName().equals(metadataField)) {
      Node typeAttribute = node.getAttributes().getNamedItem(typeName);
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
    if (node != null)
      return node;
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

    Map<String, Collection<Serializable>> keyValues = valueMap;
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
