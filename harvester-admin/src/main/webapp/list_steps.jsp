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
        <title>Steps</title>
    </head>
    <body>
        <f:view>
            <h:outputText style="h1">Steps</h:outputText>
            <h:form>
                <%@ include file="fragments/navigation_panel.jsp" %>
                <br>
                <h:outputText value="Add new : "/>
                <h:commandLink styleClass="navigation"  value="Split" action="#{stepController.prepareSplitStep}" />                
                <h:outputText value=", "/>
                <h:commandLink styleClass="navigation"  value="Transformation (XSL)" action="#{stepController.prepareXsltStep}" />                
                <h:outputText value=", "/>
                <h:commandLink styleClass="navigation" value="Validation (XSD)" action="#{stepController.prepareValidationStep}" />
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
                <h5>Transformation Steps:</h5>
                <h:dataTable value="#{stepController.steps}" var="item" columnClasses="right,left,center,left,center,center,center,center">
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
                            <h:outputText value="Step Type" />
                        </f:facet> 
                        <h:outputText value="#{item.type}"></h:outputText>
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Input Format" />
                        </f:facet> 
                        <h:outputText value="#{item.inputFormat}"></h:outputText>
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Output Format" />
                        </f:facet> 
                        <h:outputText value="#{item.outputFormat}"></h:outputText>
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Enabled" />
                        </f:facet> 
                        <h:graphicImage alt="Enabled" height="16" url="/images/#{item.enabledDisplay}.png" />
                    </h:column>
                    <h:column>
                        <f:facet name="header">
                            <h:outputText value="Actions" />
                        </f:facet> 
                        <h:commandLink styleClass="action" action="#{stepController.prepareToEdit}">
                            <f:param name="id" value="#{item.id}"/>
                            <h:graphicImage title="Edit" alt="Edit" height="16" url="/images/edit.png" />
                        </h:commandLink>
                        <h:commandLink styleClass="action" action="#{stepController.delete}"
                            onclick="return confirm('Are you sure?');">
                            <f:param name="id" value="#{item.id}"/>
                            <h:graphicImage title="Delete" alt="Delete" height="16" url="/images/delete.png" />                            
                        </h:commandLink>
                    </h:column>
                </h:dataTable>
            </h:form>
        </f:view>
    </body>
</html>
