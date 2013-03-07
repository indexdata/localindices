package ORG.oclc.oai.harvester2.verb;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;

public class XPathHelper<T> {
  QName qname;
  NamespaceContext nsContext = null;

  public XPathHelper(QName type) {
    qname = type;
  }
  
  public XPathHelper(QName type, NamespaceContext context) {
    qname = type;
    nsContext = context;
  }

  @SuppressWarnings("unchecked")
  T evaluate(Node node, String xpath) throws XPathExpressionException {
    XPathExpression expr = HarvesterVerb.xPathExprMap.get(xpath);
    if (expr == null) {
      XPath xPath = HarvesterVerb.createXPath();
      if (nsContext != null)
	xPath.setNamespaceContext(nsContext);
      expr = xPath.compile(xpath);
      HarvesterVerb.xPathExprMap.put(xpath, expr);
    }
    return (T) expr.evaluate(node, qname);
  }
}
