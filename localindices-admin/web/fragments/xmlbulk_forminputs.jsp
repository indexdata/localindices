<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="xmlbulk_form_inputs">
    <h5>XML bulk specific information:</h5>
    <h:panelGrid columns="2">
        <h:outputText value="URLs (space-separated):"/>
        <h:inputText value="#{resourceController.resource.url}"/>
        <h:outputText value="Expected Schema:"/>
        <h:inputText value="#{resourceController.resource.expectedSchema}"/>
        <h:outputText value="Normalization Filter:"/>
        <h:inputText value="#{resourceController.resource.normalizationFilter}"/>
    </h:panelGrid>
</f:subview>