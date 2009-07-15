<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="oaipmh_form_inputs">
    <h5>OAI-PMH specific information: </h5>
    <h:panelGrid columns="2">
        <h:outputText value="OAI Repository URL:"/>
        <h:inputText value="#{resourceController.resource.url}" size="50"/>
        <h:outputText value="OAI Set Name:"/>
        <h:inputText value="#{resourceController.resource.oaiSetName}" size="50"/>
        <h:outputText value="Metadata Prefix: "/>
        <h:selectOneMenu value="#{resourceController.resource.metadataPrefix}">
            <f:selectItems value="#{resourceController.metadataPrefixes}" />
        </h:selectOneMenu>
        <!--
        <h:outputText value="Metadata Schema URI: "/>
        <h:inputText value="#{resourceController.resource.schemaURI}"/>
        <h:outputText value="Normalization Filter: "/>
        <h:inputText value="#{resourceController.resource.normalizationFilter}"/>
        -->
        <h:outputText value="Use long date format:"/>
        <h:selectBooleanCheckbox value="#{resourceController.longDate}"/>
        <h:outputText value="Harvest from (dd/mm/yyyy):"/>
        <h:inputText value="#{resourceController.resource.fromDate}" size="8">
            <f:convertDateTime pattern="dd/MM/yyyy" />
        </h:inputText>
    </h:panelGrid>
</f:subview>