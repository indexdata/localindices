<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="generic_transformation_edit_buttons">
    <h5>Transformation Actions:</h5>
    <h:commandButton value="Save Changes & Exit" action="#{transformationController.save}"/>
    <h:commandButton value="Cancel Changes & Exit" action="#{transformationController.list}"/>
    <h:commandButton onclick="return confirm('Are you sure?');" value="Delete Transformation and all steps?" action="#{transformationController.saveAndPurge}"/>
    <br/>
</f:subview>
    