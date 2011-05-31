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
        <title>Available Transformations</title>
    </head>
    <body>
        <f:view>
            <h:form>
                <h:commandLink value="Back" action="#{transformationController.back}" />
                <h:commandLink value="Refresh List" action="#{transformationController.list}" />
                <h:outputText value=" | Add new transformation: "/>
                <h:commandLink value="XSLT" action="#{transformationController.prepareXsltTransformationToAdd}" />                
                <h:outputText value=" "/>
            </h:form>
            <h:form>
                <h3><h:outputText value="Available Transformations:" /></h3>
                <div id="pager">
                    <h:outputText value="Item #{transformationController.firstItem + 1}..#{transformationController.lastItem} of #{transformationController.itemCount}"/>&nbsp;
                    <h:commandLink action="#{transformationController.prev}" value="Previous #{transformationController.batchSize}" rendered="#{transformationController.firstItem >= transformationController.batchSize}"/>&nbsp;
                    <h:commandLink action="#{transformationController.next}" value="Next #{transformationController.batchSize}" rendered="#{transformationController.lastItem + transformationController.batchSize <= transformationController.itemCount}"/>&nbsp;
                    <h:commandLink action="#{transformationController.next}" value="Remaining #{transformationController.itemCount - transformationController.lastItem}"
                                   rendered="#{transformationController.lastItem < transformationController.itemCount && transformationController.lastItem + transformationController.batchSize > transformationController.itemCount}"/>
                </div>               
                <h:dataTable value="#{transformationController.transformations}" var="item" columnClasses="right,left,center,left,center,center,center,center">
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
                            <h:outputText value="Enabled" />
                        </f:facet> 
                        <h:outputText value="#{item.enabled}"></h:outputText>
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Available Actions" />
                        </f:facet> 
                        <h:commandLink value="Edit" action="#{transformationController.prepareToEdit}">
                            <f:param name="id" value="#{item.id}"/>
                        </h:commandLink>
                        <h:outputText value=" | "/>
                        <h:commandLink value="Delete" action="#{transformationController.delete}"
                            onclick="return confirm('Are you sure?');">
                            <f:param name="id" value="#{item.id}"/>
                        </h:commandLink>
                    </h:column>
                </h:dataTable>
            </h:form>
        </f:view>
    </body>
</html>
