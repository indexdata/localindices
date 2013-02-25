<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="oaipmh_form_inputs">
    <h5>OAI-PMH specific information: </h5>
    <h:panelGrid columns="2">
        <h:outputText value="Connector Engine URL:"/>
        <h:inputText  value="#{resourceController.resource.url}" size="50"/>
        <h:outputText value="User Name:"/>
        <h:inputText  value="#{resourceController.resource.username}" size="50"/>
        <h:outputText value="Password: "/>
        <h:inputText  value="#{resourceController.resource.password}" size="50"/>
        <h:outputText value="Init Data (optional)" /> 
        <h:inputTextarea cols="60" rows="4" value="#{resourceController.resource.initData}"/>
        <h:outputText value="Harvest from (dd/mm/yyyy):"/>
        <h:inputText value="#{resourceController.resource.fromDate}" size="8">
            <f:convertDateTime pattern="dd/MM/yyyy" />
        </h:inputText>
        <h:outputText value="Harvest until (dd/mm/yyyy):"/>
        <h:inputText value="#{resourceController.resource.untilDate}" size="8">
            <f:convertDateTime pattern="dd/MM/yyyy" />
        </h:inputText>
        <h:outputText value="Resumption token: (overrrides date)"/>
        <h:inputText value="#{resourceController.resource.resumptionToken}"/>
    </h:panelGrid>
</f:subview>