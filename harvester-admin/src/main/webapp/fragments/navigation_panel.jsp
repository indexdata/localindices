<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>
<f:subview id="navigation">
    <h:commandLink styleClass="navigation" value="Resources"       action="#{resourceController.listResources}" />
    <h:commandLink styleClass="navigation" value="Storages"        action="#{storageController.listStorages}" />
    <h:commandLink styleClass="navigation" value="Transformations" action="#{transformationController.list}" />
    <h:commandLink styleClass="navigation"  value="Steps"          action="#{stepController.listSteps}" />
</f:subview>
