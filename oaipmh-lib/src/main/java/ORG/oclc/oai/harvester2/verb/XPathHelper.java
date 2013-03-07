package ORG.oclc.oai.harvester2.verb;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;

class XPathHelper<T> {
  QName qname;

  XPathHelper(QName type) {
    qname = type;
  }

  @SuppressWarnings("unchecked")
  T evaluate(Node node, String xpath) throws XPathExpressionException {
    XPathExpression expr = HarvesterVerb.xPathExprMap.get(xpath);
    if (expr == null) {
      XPath xPath = HarvesterVerb.createXPath();
      xPath.setNamespaceContext(new OaiPmhNamespaceContext());
      expr = xPath.compile(xpath);
      HarvesterVerb.xPathExprMap.put(xpath, expr);
    }
    return (T) expr.evaluate(node, qname);
  }
}
