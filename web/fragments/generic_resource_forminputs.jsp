<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="generic_resource_form_inputs">
    <h5>General information: </h5>
    <h:panelGrid columns="2">
        <h:outputText value="Name:"/>
        <h:inputText value="#{resourceController.resource.name}"/>
        <h:outputText value="Title:"/>
        <h:inputText value="#{resourceController.resource.title}" />
        <h:outputText value="Description:"/>
        <h:inputText value="#{resourceController.resource.description}"/>
        <h:outputText value="Maximum records:"/>
        <h:inputText value="#{resourceController.resource.maxDbSize}" />
        <h:outputText value="Harvest schedule:"/>
        Harvest 
        <h:selectOneMenu value="#{resourceController.dayOfMonth}">
                    <f:selectItems value="#{resourceController.daysOfMonth}" />
        </h:selectOneMenu>
        (day) of
        <h:selectOneMenu value="#{resourceController.month}">
                    <f:selectItems value="#{resourceController.months}" />
        </h:selectOneMenu>
        (month) if it's
        <h:selectOneMenu value="#{resourceController.dayOfWeek}">
                    <f:selectItems value="#{resourceController.daysOfWeek}" />
        </h:selectOneMenu>
        (day of the week).
        <h:outputText value="Harvester enabled:"/>
        <h:selectBooleanCheckbox value="#{resourceController.resource.enabled}"/>
    </h:panelGrid>
</f:subview>
