<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="step_form_buttons"> 
        <h:outputText value=" Edit Step Action: " />
        <h:commandLink value=" Save" action="#{stepController.save}">
            <f:param name="id" value="#{item.id}" />
        </h:commandLink>
        <h:commandLink value=" Cancel" action="#{stepController.cancel}">
            <f:param name="id" value="#{item.id}" />
        </h:commandLink>
</f:subview>