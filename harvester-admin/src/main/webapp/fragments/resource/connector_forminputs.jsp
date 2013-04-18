<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="connector_form_inputs">
    <h5>Connector specific information: </h5>
    <h:panelGrid columns="2">
        <h:outputText value="Connector Engine URL:" styleClass="required" />
        <h:inputText  value="#{resourceController.resource.url}" size="50" styleClass="requiredInput"  />
        <h:outputText value="Connector (e.g repo) URL:"  styleClass="required"/> 
        <h:inputText value="#{resourceController.resource.connectorUrl}" size="50" styleClass="requiredInput"/>
        <h:outputText value="User Name:" styleClass="optionalInput"/>
        <h:inputText  value="#{resourceController.resource.username}" size="20" styleClass="optionalInput" />
        <h:outputText value="Password:" styleClass="optionalInput" />
        <h:inputText  value="#{resourceController.resource.password}" size="20" styleClass="optionalInput"/>
        <h:outputText value="Init Data" styleClass="optionalInput"/> 
        <h:inputTextarea cols="60" rows="4" value="#{resourceController.resource.initData}" styleClass="optionalInput"/>
        <h:outputText value="Harvest from (dd/mm/yyyy):" styleClass="optionalInput"/>
        <h:inputText value="#{resourceController.resource.fromDate}" size="8" styleClass="optionalInput">
            <f:convertDateTime pattern="dd/MM/yyyy" />
        </h:inputText>
        <h:outputText value="Harvest until (dd/mm/yyyy):" styleClass="optionalInput"/>
        <h:inputText value="#{resourceController.resource.untilDate}" size="8" styleClass="optionalInput">
            <f:convertDateTime pattern="dd/MM/yyyy" />
        </h:inputText>
        <h:outputText value="Resumption token:" styleClass="optionalInput"/>
        <h:inputText value="#{resourceController.resource.resumptionToken}" styleClass="optionalInput"/>
        <h:outputText value="Delay between requests (milliseconds):" styleClass="optionalInput" />
        <h:inputText value="#{resourceController.resource.sleep}" styleClass="optionalInput"/>
    </h:panelGrid>
</f:subview>