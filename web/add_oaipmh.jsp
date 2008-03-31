<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" type="text/css" href="css/styles.css"/>
        <title>Add a new OAI-PMH resource</title>
    </head>
    <body>
        <f:view>
            <a href="faces/list_resources.jsp">Resource List</a>
            <h:form>
                <h3>New OAI-PMH resource: </h3>
                <%@ include file="fragments/generic_resource_forminputs.jsp" %>
                <%@ include file="fragments/oaipmh_forminputs.jsp" %>
                <h:commandButton value="Add" action="#{resourceController.addResource}"/>
            </h:form>
        </f:view>
    </body>
</html>
