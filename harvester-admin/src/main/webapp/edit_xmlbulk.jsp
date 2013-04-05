<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" type="text/css" href="/css/styles.css"/>
        <title>Edit XML/MARC bulk resource</title>
    </head>
    <body>
        <f:view>
            <h:form>
                <h:commandLink styleClass="navigation" value="Resource List" action="#{resourceController.listResources}" />
                <h3>Edit XML bulk resource:</h3>
                <%@ include file="fragments/resource/generic_resource_forminputs.jsp" %>
                <%@ include file="fragments/resource/xmlbulk_forminputs.jsp" %>
                <%@ include file="fragments/generic_buttons.jsp" %>
            </h:form>
        </f:view>
    </body>
</html>
