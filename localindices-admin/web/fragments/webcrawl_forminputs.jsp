<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="webcrawl_form_inputs">
    <h5>WebCrawl specific information: </h5>
    <h:panelGrid columns="2">
        <h:outputText value="Harvested URIs:"/>
        <h:inputText value="#{resourceController.resource.startUrls}"/>
        <h:outputText value="Filetype mask:"/>
        <h:inputText value="#{resourceController.resource.filetypeMasks}"/>
        <h:outputText value="URI Mask:"/>
        <h:inputText value="#{resourceController.resource.uriMasks}"/>
        <h:outputText value="Recursion depth:"/>
        <h:inputText value="#{resourceController.resource.recursionDepth}"/>
    </h:panelGrid>
</f:subview>