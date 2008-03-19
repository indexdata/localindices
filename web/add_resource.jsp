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
        <title>Add new resource</title>
    </head>
    <body>
        <f:view>
            <a href="faces/list_resources.jsp">Resource List</a>
            <h:outputText value=" | "/>
            <a href="faces/add_resource.jsp">Add new resource</a>
            <h:form>
                <h:panelGrid columns="2">
                    <h:outputText value="Id"/>
                    <h:inputText value="#{oaiPmhResourceController.resource.id}" />
                    <h:outputText value="Name"/>
                    <h:inputText value="#{oaiPmhResourceController.resource.name}"/>
                </h:panelGrid>
                <h:commandButton value="Add" action="#{oaiPmhResourceController.addEditedResource}"/>
            </h:form>
        </f:view>
    </body>
</html>
