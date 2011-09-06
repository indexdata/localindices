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
        <title>Available TransformationSteps</title>
    </head>
    <body>
        <f:view>
            <h:form>
                <h:commandLink value="Overview" action="#{stepController.back}" />
                <h:commandLink value="Refresh List" action="#{stepController.list}" />
                <br>
                <h:outputText value="Add new : "/>
                <h:commandLink value="Xslt" action="#{stepController.prepareXsltStep}" />                
<!-- 
                 <h:outputText value=", "/>
                <h:commandLink value="WebCrawl" action="#{stepController.prepareWebCrawlResourceToAdd}" />       
 -->
                <h:outputText value=", "/>
                <h:commandLink value="XML bulk" action="#{stepController.prepareValidationStep}" />
                <h:outputText value=", "/>
            </h:form>
            <h:form>
                <h3><h:outputText value="Available resources:" /></h3>
                <div id="pager">
                    <h:outputText value="Item #{stepController.firstItem + 1}..#{stepController.lastItem} of #{stepController.itemCount}"/>&nbsp;
                    <h:commandLink action="#{stepController.prev}" value="Previous #{stepController.batchSize}" rendered="#{stepController.firstItem >= stepController.batchSize}"/>&nbsp;
                    <h:commandLink action="#{stepController.next}" value="Next #{stepController.batchSize}" rendered="#{stepController.lastItem + stepController.batchSize <= stepController.itemCount}"/>&nbsp;
                    <h:commandLink action="#{stepController.next}" value="Remaining #{stepController.itemCount - stepController.lastItem}"
                                   rendered="#{stepController.lastItem < stepController.itemCount && stepController.lastItem + stepController.batchSize > stepController.itemCount}"/>
                </div>               
                <h:dataTable value="#{stepController.resources}" var="item" columnClasses="right,left,center,left,center,center,center,center">
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
                        <h:outputText value="#{item.enabledDisplay}"></h:outputText>
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Available Actions" />
                        </f:facet> 
                        <h:commandLink value="Edit" action="#{stepController.prepareResourceToEdit}">
                            <f:param name="resourceId" value="#{item.id}"/>
                        </h:commandLink>
                        <h:outputText value=" | "/>
                        <h:commandLink value="Delete" action="#{stepController.deleteResource}"
                            onclick="return confirm('Are you sure?');">
                            <f:param name="resourceId" value="#{item.id}"/>
                        </h:commandLink>
                        <h:outputText value=" | "/>
                        <h:commandLink value="View Log" action="#{stepController.viewJobLog}">
                            <f:param name="resourceId" value="#{item.id}"/>
                        </h:commandLink>
                    </h:column>
                </h:dataTable>
            </h:form>
        </f:view>
    </body>
</html>
