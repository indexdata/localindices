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
        <link rel="stylesheet" type="text/css" href="css/styles.css"/>
        <title>Harvester log</title>
    </head>
    <body>
        <f:view>
            <!-- top menu, move out?-->
            <h:form>
                <h:commandLink value="Resource List" action="#{resourceController.listResources}" />
                <h:outputText value=" | Add new resource: "/>
                <h:commandLink value="OAI-PMH" action="#{resourceController.prepareOaiPmhResourceToAdd}" />
                <!--
                <h:outputText value=" : "/>
                <h:commandLink value="WebCrawl" action="#{resourceController.prepareWebCrawlResourceToAdd}" />
                -->
                <h:outputText value=" | "/>
                <h:commandLink value="MARC bulk" action="#{resourceController.prepareXmlBulkResourceToAdd}" />
                <h:outputText value=" | "/>
                Hello, <h:outputText value="#{loginManager.displayName}" />!
                <h:commandLink value="Logout" action="#{loginManager.doLogout}" />
            </h:form>
            <!-- Log file -->
            <h:form>
                <h3><h:outputText value="Harvester's logfile:" /></h3>
                <pre><%
                    FileInputStream data = new FileInputStream("/var/cache/harvested/harvester.log");
                    BufferedInputStream file = new BufferedInputStream(data);
                    int c = -1;
                    while((c = file.read()) != -1) out.write(c);
                    file.close();
                %></pre>
            </h:form>
        </f:view>
    </body>
</html>

