<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="generic_transformation_form_inputs">
    <h5>General information: </h5>
    <h:panelGrid columns="2">
        <h:outputText value="ID:"/>
        <h:outputText value="#{transformationController.transformation.id}"/>
        <h:outputText value="Name:"/>
        <h:outputText value="#{transformationController.transformation.name}"/>
    </h:panelGrid>
</f:subview>
