<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="generic_transformation_form_inputs">
    <h5>General information: </h5>
    <h:panelGrid columns="2">
        <h:outputText value="Name:"/>
        <h:inputText value="#{transformationController.transformation.name}" size="30"/>
        <h:outputText value="Description:"/>
        <h:inputTextarea cols="60" rows="3" value="#{transformationController.transformation.description}"/>
        <h:outputText value="Enabled:"/>
        <h:selectBooleanCheckbox value="#{transformationController.transformation.enabled}"/>
    </h:panelGrid>
</f:subview>
