<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
    <%@ include file="../fragments/transformation/generic_head.jsp" %>
    <body onload="hideEditStep();">
        <f:view>
            <h:form>
                <h:commandLink styleClass="navigation" value="Transformations List" action="#{transformationController.list}" />
                <h3>Edit Transformation: </h3>
                <%@ include file="../fragments/transformation/generic_information.jsp" %>
                <%@ include file="../fragments/transformation/steps_select.jsp" %>
            </h:form>
        </f:view>
    </body>
</html>
