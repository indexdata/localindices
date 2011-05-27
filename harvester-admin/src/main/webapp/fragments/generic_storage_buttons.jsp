<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="generic_edit_buttons">
    <h5>Actions: </h5>
    <h:commandButton value="Save Changes & Exit" action="#{storageController.saveStorage}"/>
    <h:commandButton value="Cancel Changes & Exit" action="#{storageController.listStorages}"/>
    <h:commandButton onclick="return confirm('Are you sure?');" value="Delete All Records" action="#{storageController.saveAndPurge}"/>
    <br/>
</f:subview>
