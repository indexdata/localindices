<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="solr_form_inputs">
    <h5>SOLR specific information: </h5>
    <h:panelGrid columns="2">
        <h:outputText value="Basic SOLR Server URL:"/>
        <h:inputText value="#{storageController.storage.url}" size="100"/>
<!-- 
        <h:inputText value="#{storageController.storage.transformation}"/>
 -->
    </h:panelGrid>
</f:subview>