<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page language="java" import="java.io.*"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" type="text/css" href="../css/styles.css"/>
        <title>Harvester log</title>
		<script type="text/javascript">
		    function goToBottom() {
		  location.href = "#bottom";
		}
		</script>        
    </head>
    <body  onload="goToBottom();">
        <f:view>
            <!-- top menu, move out?-->
            <h:form>
                <h:commandLink styleClass="navigation" value="Back to Resources" action="#{resourceController.listResources}" />
                <h:commandLink styleClass="navigation" value="Refresh" action="#{resourceController.viewJobLog}">
                        <f:param name="resourceId" value="#{resourceController.currentId}" />
                        <f:param name="resourceId" value="#{resourceController.currentId}" />
                </h:commandLink>
               <a class="navigation" href="#bottom">Bottom</a>
            </h:form>
            <!-- Log file -->
            <h:form>
                <h3><h:outputText value="Harvester's logfile:" /></h3>
                <pre><h:outputText value="#{resourceController.jobLog}" /></pre>
            </h:form>
            <h:form>
                <h:commandLink styleClass="navigation" value="Back to Resources" action="#{resourceController.listResources}" />
                <h:commandLink styleClass="navigation" value="Refresh log" action="#{resourceController.viewJobLog}">
                        <f:param name="resourceId" value="#{resourceController.currentId}" />
                        <f:param name="location" value="bottom" />
                </h:commandLink>
            </h:form>
            <a name="bottom"></a>
        </f:view>
    </body>
</html>

