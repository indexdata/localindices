<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<%--
    This file is an entry point for JavaServer Faces application.
--%>

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" type="text/css" href="styles.css"/>
        <title>Add new resource</title>
    </head>
    <body>
        <f:view>
            <a href="faces/list_resources.jsp">Resource List</a>
            <h:form>
                <h3>New resource:</h3>
                <h:panelGrid columns="2">
                    <h:outputText value="Name:"/>
                    <h:inputText value="#{resourceController.resource.name}"/>
                    <h:outputText value="Title:"/>
                    <h:inputText value="#{resourceController.resource.title}" />
                    <h:outputText value="Description:"/>
                    <h:inputText value="#{resourceController.resource.description}"/>
                    <h:outputText value="Maximum records:"/>
                    <h:inputText value="#{resourceController.resource.maxDbSize}" />
                </h:panelGrid>
                <h:commandButton value="Add" action="#{resourceController.addEditedResource}"/>
            </h:form>
        </f:view>
    </body>
</html>
