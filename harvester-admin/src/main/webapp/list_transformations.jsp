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
        <title>Transformations</title>
    </head>
    <body>
        <f:view>
            <h:form>
                <%@ include file="fragments/navigation_panel.jsp" %>
                <h3>
                    <h:outputText>Transformations</h:outputText>
                </h3>
                <h:outputText value="Add new transformation: "/>
                <h:commandLink styleClass="navigation"  value="XSLT" action="#{transformationController.prepareXsltTransformationToAdd}" />                
                <h:outputText value=" "/>
            </h:form>
            <h:form>
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
                        <h:commandLink styleClass="action" action="#{transformationController.prepareToEdit}">
                            <f:param name="id" value="#{item.id}"/>
                            <h:graphicImage alt="Edit" height="16" url="/images/edit.png" />
                        </h:commandLink>
                        <h:commandLink styleClass="action" action="#{transformationController.delete}"
                            onclick="return confirm('Are you sure?');">
                            <f:param name="id" value="#{item.id}"/>
                            <h:graphicImage alt="Delete" height="16" url="/images/delete.png" />
                        </h:commandLink>
                    </h:column>
                </h:dataTable>
            </h:form>
        </f:view>
    </body>
</html>
