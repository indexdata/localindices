<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="generic_edit_buttons">
    <h:commandButton value="Save Changes & Exit" action="#{resourceController.saveResource}"/>
    <h:commandButton value="Cancel Changes & Exit" action="list_resources"/>
    <h:commandButton onclick="return confirm('Are you sure?');" value="Delete Harvested Records" action="#{resourceController.saveAndPurge}"/>
</f:subview>
