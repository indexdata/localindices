<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" type="text/css" href="styles.css"/>
        <title>Add a new OAI-PMH resource</title>
    </head>
    <body>
        <f:view>
            <a href="faces/list_resources.jsp">Resource List</a>
            <h:form>
                <h3>New OAI-PMH resource: </h3>
                <%@ include file="add_resource.jsp" %>
                <h5>OAI-PMH specific information: </h5>
                <h:panelGrid columns="2">
                    <h:outputText value="OAI Set Name:"/>
                    <h:inputText value="#{resourceController.resource.oaiSetName}"/>
                    <h:outputText value="Metadata Prefix: "/>
                    <h:inputText value="#{resourceController.resource.metadataPrefix}" />
                    <h:outputText value="Metdata Schema URI: "/>
                    <h:inputText value="#{resourceController.resource.schemaURI}"/>
                    <h:outputText value="Normalization Filter: "/>
                    <h:inputText value="#{resourceController.resource.normalizationFilter}" />
                </h:panelGrid>
                <h:commandButton value="Add" action="#{resourceController.addEditedResource}"/>
            </h:form>
        </f:view>
    </body>
</html>
