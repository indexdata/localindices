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
        <title>Available resources</title>
    </head>
    <body>
        <f:view>
            <h:form>
                <h:commandLink value="Refresh List" action="#{resourceController.listResources}" />
                <h:outputText value=" | Add new resource: "/>
                <h:commandLink value="OAI-PMH" action="#{resourceController.prepareOaiPmhResourceToAdd}" />                
                <h:outputText value=", "/>
                <h:commandLink value="WebCrawl" action="#{resourceController.prepareWebCrawlResourceToAdd}" />       
                <h:outputText value=", "/>
                <h:commandLink value="MARC bulk" action="#{resourceController.prepareXmlBulkResourceToAdd}" />
                <!-- NELLCO doesn't want logout
                <h:outputText value=" | "/>
                Hello, <h:outputText value="#{loginManager.displayName}" />!
                <h:commandLink value="Logout" action="#{loginManager.doLogout}" />
                -->
            </h:form>
            <h:form>
                <h3><h:outputText value="Available resources:" /></h3>
                <div id="pager">
                    <h:outputText value="Item #{resourceController.firstItem + 1}..#{resourceController.lastItem} of #{resourceController.itemCount}"/>&nbsp;
                    <h:commandLink action="#{resourceController.prev}" value="Previous #{resourceController.batchSize}" rendered="#{resourceController.firstItem >= resourceController.batchSize}"/>&nbsp;
                    <h:commandLink action="#{resourceController.next}" value="Next #{resourceController.batchSize}" rendered="#{resourceController.lastItem + resourceController.batchSize <= resourceController.itemCount}"/>&nbsp;
                    <h:commandLink action="#{resourceController.next}" value="Remaining #{resourceController.itemCount - resourceController.lastItem}"
                                   rendered="#{resourceController.lastItem < resourceController.itemCount && resourceController.lastItem + resourceController.batchSize > resourceController.itemCount}"/>
                </div>               
                <h:dataTable value="#{resourceController.resources}" var="item" columnClasses="right,left,center,left,center,center,center,center">
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="ID" />
                        </f:facet> 
                        <h:outputText value="#{item.id}"></h:outputText>
                    </h:column>                
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Name" />
                        </f:facet> 
                        <h:outputText value="#{item.name}"></h:outputText>
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Status" />
                        </f:facet> 
                        <h:outputText value="#{item.currentStatus}"></h:outputText>
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Status Msg" />
                        </f:facet> 
                        <h:outputText value="#{item.message}"></h:outputText>
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Last Harvested" />
                        </f:facet>                        
                        <h:outputText rendered="#{item.lastHarvestFinished != null}" value="#{item.lastHarvestFinished}"/>
                        <h:outputText rendered="#{item.lastHarvestFinished == null}" value="attempted on #{item.lastHarvestStarted}"/>
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Next Scheduled Harvest" />
                        </f:facet> 
                        <h:outputText value="#{item.nextHarvestSchedule}"></h:outputText>
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Enabled" />
                        </f:facet> 
                        <h:outputText value="#{item.enabled}"></h:outputText>
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Available Actions" />
                        </f:facet> 
                        <h:commandLink value="Edit" action="#{resourceController.prepareResourceToEdit}">
                            <f:param name="resourceId" value="#{item.id}"/>
                        </h:commandLink>
                        <h:outputText value=" | "/>
                        <h:commandLink value="Delete" action="#{resourceController.deleteResource}"
                            onclick="return confirm('Are you sure?');">
                            <f:param name="resourceId" value="#{item.id}"/>
                        </h:commandLink>
                        <h:outputText value=" | "/>
                        <h:commandLink value="View Log" action="#{resourceController.viewJobLog}">
                            <f:param name="resourceId" value="#{item.id}"/>
                        </h:commandLink>
                    </h:column>
                </h:dataTable>
            </h:form>
        </f:view>
    </body>
</html>
