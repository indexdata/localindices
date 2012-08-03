<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="step_generic_forminputs">
        <h:panelGrid columns="2">
            <h:outputText value="Step Name:" />
            <h:inputText value="#{stepController.current.name}" />
            <h:outputText value="Description:" />
            <h:inputText value="#{stepController.current.description}" />
            <h:outputText value="Type:" />
            <h:inputText value="#{stepController.current.type}" />
            <h:outputText value="Input format:" />
            <h:inputText value="#{stepController.current.inputFormat}" />
            <h:outputText value="Output format:" />
            <h:inputText value="#{stepController.current.outputFormat}" />
        </h:panelGrid>
</f:subview>
        