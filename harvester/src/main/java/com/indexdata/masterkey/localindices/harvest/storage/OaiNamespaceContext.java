package com.indexdata.masterkey.localindices.harvest.storage;

import java.util.Arrays;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

public class OaiNamespaceContext implements NamespaceContext {

    @Override
    public String getNamespaceURI(String prefix) {
        return "http://www.openarchives.org/OAI/2.0/";    }

    @Override
    public String getPrefix(String namespaceURI) {
        return "oai20";
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        return Arrays.asList("oai20").iterator();
    }

}