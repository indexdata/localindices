<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="generic_storage_form_inputs">
    <h5>General information: </h5>
    <h:panelGrid columns="2">
        <h:outputText value="ID:"/>
        <h:outputText value="#{storageController.storage.id}"/>
        <h:outputText value="Name:"/>
        <h:inputText value="#{storageController.storage.name}" size="30"/>
        <h:outputText value="Storage Description:"/>
        <h:inputTextarea cols="60" rows="3" value="#{storageController.storage.description}"/>
        <h:outputText value="Enabled:"/>
        <h:selectBooleanCheckbox value="#{storageController.storage.enabled}"/>
        <!-- List box of Transformations pipeline-->
    </h:panelGrid>
</f:subview>
