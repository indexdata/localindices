/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Some XML helper methods to hide DOM complexity.
 * @author jakub
 */
public class XmlUtils {
    private static DocumentBuilder builder;
    private static Transformer transformer;
    
    private static DocumentBuilder getBuilder() throws ParserConfigurationException {
        if (builder == null) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);
            builder = factory.newDocumentBuilder();
        }
        return builder;
    }
    
    private static Transformer getTransformer() throws TransformerConfigurationException {
        if (transformer == null) {
            TransformerFactory factory = TransformerFactory.newInstance();
            transformer = factory.newTransformer();
        }
        return transformer;
    }

    public static Document newDoc(String rootNode) throws ParserConfigurationException {
        Document doc = getBuilder().newDocument();
        Element root = doc.createElement(rootNode);
        doc.appendChild(root);
        return doc;
    }
    
    public static Document parse(InputStream source) 
            throws ParserConfigurationException, SAXException, IOException {
        return getBuilder().parse(source);
    }
    
    public static Document parse(String path) 
            throws ParserConfigurationException, SAXException, IOException {
        return getBuilder().parse(path);
    }
    
    public static Document parse(StringReader reader) throws ParserConfigurationException, SAXException, IOException {
        return getBuilder().parse(new InputSource(reader));
    }
    
    public static void serialize(Document doc, OutputStream dest) throws TransformerConfigurationException, TransformerException {
        getTransformer().transform(new DOMSource(doc), new StreamResult(dest));
    }

    /**
     * Escape five, basic XML entities.
     * @param s string to be escaped
     * @return xml-escaped string
     */
    public static String escape(String s) {
        StringBuilder result = new StringBuilder();
        StringCharacterIterator i = new StringCharacterIterator(s);
        char c =  i.current();
        while (c != CharacterIterator.DONE ){
            switch (c) {
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
                    break;
                case '"':
                    result.append("&quot;");
                    break;
                case '\'':
                    result.append("&apos;");
                    break;
                case '&':
                    result.append("&amp;");
                    break;
                default:
                    result.append(c);
            }
            c = i.next();
        }
        return result.toString();
    }
    
    public static Node appendTextNode(Node parent, String tagName, String text) {
        Document doc = parent.getOwnerDocument();
        Node newNode = doc.createElement(tagName);
        newNode.setTextContent(text);
        parent.appendChild(newNode);
        return newNode;
    }
}
