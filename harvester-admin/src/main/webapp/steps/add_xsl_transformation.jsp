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
        <title>Add new Transformation Pipeline</title>
    </head>
    <body>
        <f:view>
            <h:form>
                <h:commandLink styleClass="navigation"  value="Transformation List" action="#{transformationController.list}" />
                <h3>New XSL Transformation: </h3>
                <%@ include file="/fragments/transformation/generic_forminputs.jsp" %>
                <%@ include file="/fragments/transformation/xslt_transformation_forminputs.jsp" %>
                <h:commandButton value="Add" action="#{transformationController.add}"/>
            </h:form>
        </f:view>
    </body>
</html>
