<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<%--
    This file is an entry point for JavaServer Faces application.
--%>

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>All resources</title>
    </head>
    <body>
        <f:view>
        <a href="faces/list_resources.jsp">Resource List</a>
        <h:outputText value=" | "/>
        <a href="faces/add_resource.jsp">Add new resource</a>
            <h2><h:outputText value="Available resources:" /></h2>
            <h:form>
                <h:outputText value="Item #{oaiPmhResourceController.firstItem + 1}..#{oaiPmhResourceController.lastItem} of #{oaiPmhResourceController.itemCount}"/>&nbsp;
                <h:commandLink action="#{oaiPmhResourceController.prev}" value="Previous #{oaiPmhResourceController.batchSize}" rendered="#{oaiPmhResourceController.firstItem >= oaiPmhResourceController.batchSize}"/>&nbsp;
                <h:commandLink action="#{oaiPmhResourceController.next}" value="Next #{oaiPmhResourceController.batchSize}" rendered="#{oaiPmhResourceController.lastItem + oaiPmhResourceController.batchSize <= oaiPmhResourceController.itemCount}"/>&nbsp;
                <h:commandLink action="#{oaiPmhResourceController.next}" value="Remaining #{oaiPmhResourceController.itemCount - oaiPmhResourceController.lastItem}"
                               rendered="#{oaiPmhResourceController.lastItem < oaiPmhResourceController.itemCount && oaiPmhResourceController.lastItem + oaiPmhResourceController.batchSize > oaiPmhResourceController.itemCount}"/>
                <h:dataTable value="#{oaiPmhResourceController.resources}" var="item">
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Resource id" />
                        </f:facet> 
                        <h:outputText value="#{item.id}"></h:outputText>
                    </h:column>                
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Resource name" />
                        </f:facet> 
                        <h:outputText value="#{item.name}"></h:outputText>
                    </h:column>                
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Last Updated" />
                        </f:facet> 
                        <h:outputText value="#{item.lastUpdated}"></h:outputText>
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Available Actions" />
                        </f:facet> 
                        <h:commandLink value="Edit" action="edit">
                            <f:param name="prod_id" value="#{item.id}"/>
                        </h:commandLink>
                        <h:outputText value=" | "/>
                        <h:commandLink value="Delete" action="edit">
                            <f:param name="prod_id" value="#{item.id}"/>
                        </h:commandLink>
                    </h:column>
                </h:dataTable>
            </h:form>
        </f:view>
    </body>
</html>
