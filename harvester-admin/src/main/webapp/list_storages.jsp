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
        <title>Available Storages</title>
    </head>
    <body>
        <f:view>
            <h:form>
                <h:commandLink value="Refresh List" action="#{storageController.listResources}" />
                <h:outputText value=" | Add new storage: "/>
                <h:commandLink value="SOLR" action="#{storageController.prepareSOLRStorageToAdd}" />                
                <h:outputText value=", "/>
                <h:commandLink value="Zebra2" action="#{storageController.prepareZebraStorageToAdd}" />       
                <h:outputText value=" "/>
            </h:form>
            <h:form>
                <h3><h:outputText value="Available Storages:" /></h3>
                <div id="pager">
                    <h:outputText value="Item #{storageController.firstItem + 1}..#{storageController.lastItem} of #{storageController.itemCount}"/>&nbsp;
                    <h:commandLink action="#{storageController.prev}" value="Previous #{storageController.batchSize}" rendered="#{storageController.firstItem >= storageController.batchSize}"/>&nbsp;
                    <h:commandLink action="#{storageController.next}" value="Next #{storageController.batchSize}" rendered="#{storageController.lastItem + storageController.batchSize <= storageController.itemCount}"/>&nbsp;
                    <h:commandLink action="#{storageController.next}" value="Remaining #{storageController.itemCount - storageController.lastItem}"
                                   rendered="#{storageController.lastItem < storageController.itemCount && storageController.lastItem + storageController.batchSize > storageController.itemCount}"/>
                </div>               
                <h:dataTable value="#{storageController.resources}" var="item" columnClasses="right,left,center,left,center,center,center,center">
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
                        <h:commandLink value="Edit" action="#{storageController.prepareStorageToEdit}">
                            <f:param name="storageId" value="#{item.id}"/>
                        </h:commandLink>
                        <h:outputText value=" | "/>
                        <h:commandLink value="Delete" action="#{storageController.deleteStorage}"
                            onclick="return confirm('Are you sure?');">
                            <f:param name="storageId" value="#{item.id}"/>
                        </h:commandLink>
                        <h:outputText value=" | "/>
                        <h:commandLink value="View Log" action="#{storageController.viewStorageLog}">
                            <f:param name="storageId" value="#{item.id}"/>
                        </h:commandLink>
                    </h:column>
                </h:dataTable>
            </h:form>
        </f:view>
    </body>
</html>
