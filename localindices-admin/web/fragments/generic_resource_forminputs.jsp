<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="generic_resource_form_inputs">
    <h5>General information: </h5>
    <h:panelGrid columns="2">
        <h:outputText value="Name:"/>
        <h:inputText value="#{resourceController.resource.name}" size="30"/>
        <h:outputText value="Service Provider:"/>
        <h:inputText value="#{resourceController.resource.title}" size="30"/>
        <h:outputText value="Technical Notes:"/>
        <h:inputTextarea cols="60" rows="6" value="#{resourceController.resource.description}"/>
        <!-- not used
        <h:outputText value="Maximum records:"/>
        <h:inputText value="#{resourceController.resource.maxDbSize}" />
        -->
        <h:outputText value="Harvest schedule:"/>
        <h:panelGrid columns="7">
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
            (day of the week)
            <h:column>Harvesting time:</h:column>
            <h:selectOneMenu value="#{resourceController.hour}">
                    <f:selectItems value="#{resourceController.hours}" />
            </h:selectOneMenu>
            (hour in 24 format)
            <h:selectOneMenu value="#{resourceController.min}">
                    <f:selectItems value="#{resourceController.mins}" />
            </h:selectOneMenu>
            (min)
        </h:panelGrid>
        <h:outputText value="Ignore schedule, harvest now:"/>
        <h:selectBooleanCheckbox value="#{resourceController.setToNow}"/>
        <h:outputText value="Harvest job enabled:"/>
        <h:selectBooleanCheckbox value="#{resourceController.resource.enabled}"/>
    </h:panelGrid>
</f:subview>
