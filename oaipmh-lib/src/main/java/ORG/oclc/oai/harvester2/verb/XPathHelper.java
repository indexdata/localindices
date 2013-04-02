package ORG.oclc.oai.harvester2.verb;

import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressWarnings("rawtypes")
public class XPathHelper<T> {
  QName qname;
  NamespaceContext nsContext = null;
  private static HashMap<Thread, XPathFactory> xpathFactoryMap = new HashMap<Thread, XPathFactory>();
  private static HashMap<String, XPathExpression> xpathExprMap = new HashMap<String, XPathExpression>();
  private static HashMap<Class, QName> constant ; 
  static {
    constant = new LinkedHashMap<Class, QName>();
    constant.put(NodeList.class, XPathConstants.NODESET);
    constant.put(String.class,   XPathConstants.STRING);
    constant.put(Number.class,   XPathConstants.NUMBER);
    constant.put(Boolean.class,  XPathConstants.BOOLEAN);
  }
  public XPathHelper(QName type) {
    qname = type;
  }

  public XPathHelper(QName type, NamespaceContext context) {
    qname = type;
    nsContext = context;
  }

  public XPathHelper(T type) {
    qname = constant.get(type.getClass());
  }

  public XPathHelper(T type, NamespaceContext context) {
    qname = constant.get(type.getClass());
    nsContext = context;
  }

  @SuppressWarnings("rawtypes")
  public XPathHelper(Class theClass) {
    qname = constant.get(theClass);
  }
  
  @SuppressWarnings("rawtypes")
  public XPathHelper(Class theClass, NamespaceContext context) {
    qname = constant.get(theClass);
    nsContext = context;
  }

  static synchronized XPath createXPath() {
    /* create transformer */
    XPathFactory xpathFactory = xpathFactoryMap.get(Thread.currentThread());
    if (xpathFactory == null) {
	xpathFactory = XPathFactory.newInstance();
	xpathFactoryMap.put(Thread.currentThread(), xpathFactory);
    }
    return xpathFactory.newXPath();
  }

  @SuppressWarnings("unchecked")
  public T evaluate(Node node, String xpath) throws XPathExpressionException {
    XPathExpression expr = xpathExprMap.get(xpath);
    if (expr == null) {
      XPath xPath = createXPath();
      if (nsContext != null)
	xPath.setNamespaceContext(nsContext);
      expr = xPath.compile(xpath);
      xpathExprMap.put(xpath, expr);
    }
    return (T) expr.evaluate(node, qname);
  }
}
