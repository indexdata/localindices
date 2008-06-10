<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="oaipmh_form_inputs">
    <h5>OAI-PMH specific information: </h5>
    <h:panelGrid columns="2">
        <h:outputText value="Harvest from (dd/MM/yyyy):"/>
        <h:inputText value="#{resourceController.resource.fromDate}">
            <f:convertDateTime pattern="dd/MM/yyyy" />
        </h:inputText>
        <h:outputText value="OAI Repository URL:"/>
        <h:inputText value="#{resourceController.resource.url}" disabled="#{resourceController.lastOutcome == 'update'}"/>
        <h:outputText value="OAI Set Name:"/>
        <h:inputText value="#{resourceController.resource.oaiSetName}" disabled="#{resourceController.lastOutcome == 'update'}"/>
        <h:outputText value="Metadata Prefix: "/>
        <h:inputText value="#{resourceController.resource.metadataPrefix}" disabled="#{resourceController.lastOutcome == 'update'}"/>
        <h:outputText value="Metdata Schema URI: "/>
        <h:inputText value="#{resourceController.resource.schemaURI}" disabled="#{resourceController.lastOutcome == 'update'}"/>
        <h:outputText value="Normalization Filter: "/>
        <h:inputText value="#{resourceController.resource.normalizationFilter}" disabled="#{resourceController.lastOutcome == 'update'}"/>
    </h:panelGrid>
</f:subview>