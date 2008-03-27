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
        <title>Add a new WebCrawl resource</title>
    </head>
    <body>
        <f:view>
            <a href="faces/list_resources.jsp">Resource List</a>
            <h:form>
                <h3>New WebCrawl resource:</h3>
                <%@ include file="add_resource.jsp" %>
                <h5>WebCrawl specific information: </h5>
                <h:panelGrid columns="2">
                    <h:outputText value="Harvested URIs:"/>
                    <h:inputText value="#{resourceController.resource.harvestedUrls}"/>
                    <h:outputText value="Filetype mask:"/>
                    <h:inputText value="#{resourceController.resource.filetypeMask}" />
                    <h:outputText value="URI Mask:"/>
                    <h:inputText value="#{resourceController.resource.uriMask}"/>
                </h:panelGrid>
                <h:commandButton value="Add" action="#{resourceController.addEditedResource}"/>
            </h:form>
        </f:view>
    </body>
</html>
