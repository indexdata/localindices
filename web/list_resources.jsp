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
                <a href="faces/list_resources.jsp">Resource List</a>
                <h:outputText value=" | Add new resource: "/>
                <h:commandLink value="OAI-PMH" action="#{resourceController.prepareOaiPmhResourceToAdd}" />
                <h:outputText value=" | "/>
                <h:commandLink value="WebCrawl" action="#{resourceController.prepareWebCrawlResourceToAdd}" />
                <h:outputText value=" | "/>
                <h:commandLink value="XML bulk" action="#{resourceController.prepareXmlBulkResourceToAdd}" />
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
                <h:dataTable value="#{resourceController.resources}" var="item" columnClasses="first_in_row,number,number,number,action">
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Name: " />
                        </f:facet> 
                        <h:outputText value="#{item.name}"></h:outputText>
                    </h:column>                
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Records: " />
                        </f:facet> 
                        <h:outputText value="#{item.recordsHarvested}"></h:outputText>
                    </h:column>                
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Last harvested at: " />
                        </f:facet> 
                        <h:outputText value="#{item.lastHarvestStarted}"></h:outputText>
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Status info: " />
                        </f:facet> 
                        <h:outputText value="#{item.currentStatus}"></h:outputText>
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Last edited: " />
                        </f:facet> 
                        <h:outputText value="#{item.lastUpdated}"></h:outputText>
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Available Actions: " />
                        </f:facet> 
                        <h:commandLink value="Edit" action="#{resourceController.prepareResourceToEdit}">
                            <f:param name="resourceId" value="#{item.id}"/>
                        </h:commandLink>
                        <h:outputText value=" | "/>
                        <h:commandLink value="Delete" action="#{resourceController.deleteResource}">
                            <f:param name="resourceId" value="#{item.id}"/>
                        </h:commandLink>
                    </h:column>
                </h:dataTable>
            </h:form>
        </f:view>
    </body>
</html>
