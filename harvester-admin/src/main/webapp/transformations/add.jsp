<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html
    xmlns:h="http://java.sun.com/jsf/html"
    xmlns:f="http://java.sun.com/jsf/core"
>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" type="text/css" href="../css/styles.css"/>
        <title>Add new Transformation Pipeline</title>
    </head>
    <body>
        <f:view>
            <h:form>
                <h:commandLink styleClass="navigation"  value="Transformation List" action="#{transformationController.list}" />
                <h3>New Transformation: </h3>
                <%@ include file="../fragments/transformation/generic_forminputs.jsp" %>
                <!--  
                <%@ include file="../fragments/transformation/step_forminputs.jsp" %>
                 -->
                <h:commandButton value="Add" action="#{transformationController.saveExit}"/>
                <h:commandButton value="Save" action="#{transformationController.save}"/>
                <h:commandButton value="Cancel" action="#{transformationController.cancel}"/>
            </h:form>
        </f:view>
    </body>
</html>
