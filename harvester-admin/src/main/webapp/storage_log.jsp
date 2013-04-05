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
        <link rel="stylesheet" type="text/css" href="/css/styles.css"/>
        <title>Storage log</title>
    </head>
    <body>
        <f:view>
            <!-- top menu, move out?-->
            <h:form>
                <h:commandLink value="Storage List" action="#{storageController.listStorages}" />
                <h:outputText value=" | Add new storage: "/>
                <h:commandLink value="Solr Index" action="#{storageController.prepareSolrStorageToAdd}" />
                <!--
                <h:outputText value=" : "/>
                <h:commandLink value="Zebra Index" action="#{storageController.prepareZebraStorageToAdd}" />
                -->
                <h:outputText value=" | "/>
                <h:commandLink value="Console Storage" action="#{storageController.prepareConsoleStorageToAdd}" />
                <h:outputText value=" | "/>
                Hello, <h:outputText value="#{loginManager.displayName}" />!
                <h:commandLink value="Logout" action="#{loginManager.doLogout}" />
            </h:form>
            <!-- Log file -->
            <h:form>
                <h3><h:outputText value="Harvester's logfile:" /></h3>
                <pre><h:outputText value="#{storageController.jobLog}" /></pre>
            </h:form>
        </f:view>
    </body>
</html>

