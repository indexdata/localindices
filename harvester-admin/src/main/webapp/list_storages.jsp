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
        <title>Storages</title>
    </head>
    <body>
        <f:view>
            <h:form>
                <%@ include file="fragments/navigation_panel.jsp" %>
                <h3>
                    <h:outputText>Storages</h:outputText>
                </h3>
                <h:outputText value="Add new storage: "/>
                <h:commandLink styleClass="navigation"  value="Solr" action="#{storageController.prepareSolrStorageToAdd}" />                
<!-- 
                <h:outputText value=", "/>
                <h:commandLink styleClass="navigation"  value="ConsoleStorage" action="#{storageController.prepareConsoleStorageToAdd}" />       
                <h:outputText value=", "/>
                <h:commandLink value="Zoo Keeper (Solr)" action="#{storageController.prepareSolrStorageToAdd}" />                
                <h:outputText value=" "/>
 -->
            </h:form>
            <h:form>
                <div id="pager">
                    <h:outputText value="Item #{storageController.firstItem + 1}..#{storageController.lastItem} of #{storageController.itemCount}" rendered="#{storageController.itemCount > 0}"/>&nbsp;
                    <h:commandLink action="#{storageController.prev}" value="Previous" rendered="#{storageController.firstItem > 1}"/>&nbsp;
                    <h:commandLink action="#{storageController.next}" value="Next" rendered="#{storageController.lastItem < storageController.itemCount}"/>&nbsp;
                </div>               
                <h:dataTable value="#{storageController.storages}" var="item" columnClasses="storagename,enabled,actions">
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Name" />
                        </f:facet> 
                        <h:outputText value="#{item.name}"></h:outputText>
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Enabled" />
                        </f:facet> 
                        <h:graphicImage alt="Enabled" height="16" url="/images/#{item.enabled}.png" />
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Actions" />
                        </f:facet> 
                        <h:commandLink action="#{storageController.prepareStorageToEdit}">
                            <f:param name="storageId" value="#{item.id}"/>
                            <h:graphicImage alt="Edit" height="16" url="/images/edit.png" />
                        </h:commandLink>
                        <h:commandLink action="#{storageController.deleteStorage}"
                            onclick="return confirm('Are you sure?');">
                            <f:param name="storageId" value="#{item.id}"/>
                            <h:graphicImage alt="Delete" height="16" url="/images/delete.png" />
                        </h:commandLink>
                    </h:column>
                </h:dataTable>
            </h:form>
        </f:view>
    </body>
</html>
