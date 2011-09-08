<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="xmlbulk_form_inputs">
    <h5>XML bulk specific information:</h5>
    <h:panelGrid columns="2">
        <h:outputText value="URLs (space-separated):"/>
        <h:inputTextarea cols="100" rows="15" value="#{resourceController.resource.url}"/>
        <h:outputText value="Split at depth (Zero disables splitting):"/>
        <h:inputText value="#{resourceController.resource.splitAt}"/>
        <h:outputText value="Split (number of records. Zero or Empty disables split):"/>
        <h:inputText value="#{resourceController.resource.splitSize}"/>
    </h:panelGrid>
</f:subview>
