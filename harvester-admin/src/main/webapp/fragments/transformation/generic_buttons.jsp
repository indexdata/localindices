<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="generic_transformation_edit_buttons">
    <h:commandButton value="Save"   action="#{transformationController.saveExit}"/>
    <h:commandButton value="Cancel" action="#{transformationController.cancel}"/>
    <h:commandButton onclick="return confirm('Are you sure?');" value="Delete Transformation..." action="#{transformationController.saveAndPurge}"/>
    <br/>
</f:subview>
    