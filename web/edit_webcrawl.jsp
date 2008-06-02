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
        <title>Edit resource</title>
    </head>
    <body>
        <f:view>
            <h:form>
                <h:commandLink value="Resource List" action="list_resources" />
                <h3>Edit WebCrawl resource:</h3>
                <%@ include file="fragments/generic_resource_forminputs.jsp" %>
                <%@ include file="fragments/webcrawl_forminputs.jsp" %>
                <h:commandButton value="Save" action="#{resourceController.saveResource}"/>
            </h:form>
        </f:view>
    </body>
</html>