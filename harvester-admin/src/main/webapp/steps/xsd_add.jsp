<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" type="text/css" href="../css/styles.css"/>
        <title>Validation Step Editor</title>
    </head>
    <body>
        <f:view>
            <h:form>
                <h:commandLink styleClass="navigation"  value="Steps" action="#{stepController.list}" />
                <h3>Validation Step: </h3>
                <%@ include file="/fragments/step/generic_forminputs.jsp" %>
                <%@ include file="/fragments/step/xsd_forminputs.jsp" %>
                <%@ include file="/fragments/step/test_forminputs.jsp" %>
                <h:commandButton value="Save" action="#{stepController.save}"/>
                <h:commandButton value="Cancel" action="#{stepController.cancel}"/>
            </h:form>
        </f:view>
    </body>
</html>
