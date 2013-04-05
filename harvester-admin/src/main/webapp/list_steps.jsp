<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" type="text/css" href="/css/styles.css"/>
        <title>Steps</title>
    </head>
    <body>
        <f:view>
            <h:form>
                <%@ include file="/fragments/navigation_panel.jsp" %>
                <h3> 
                    <h:outputText>Steps</h:outputText>
                </h3>
                <h:outputText value="Add new: "/>
                <!--
                <h:commandLink styleClass="navigation"  value="Split" action="#{stepController.prepareSplitStep}" />                
                -->
                <h:commandLink styleClass="navigation"  value="Transformation (XSL)" action="#{stepController.prepareXsltStep}" />                 
                <h:commandLink styleClass="navigation"  value="Custom Step" action="#{stepController.prepareCustomStep}">
                    <f:param name="entityClass" value="CustomTransformationStep" />
                </h:commandLink>
                <h:commandLink styleClass="navigation"  value="Xml Logging Step" action="#{stepController.prepareStep}">
                    <f:param name="entityClass" value="CustomTransformationStep" />
                    <f:param name="customClass" value="com.indexdata.masterkey.localindices.harvest.messaging.XmlLoggerRouter" />
                </h:commandLink>
                <h:commandLink styleClass="navigation"  value="Validation (XSD)" action="#{stepController.prepareStep}">
                    <f:param name="entityClass" value="CustomTransformationStep" />
                    <f:param name="customClass" value="com.indexdata.masterkey.localindices.harvest.messaging.XmlLoggerRouter" />
                </h:commandLink>
                    
            </h:form>
            <h:form>
                <div id="pager">
                    <h:outputText value="Item #{stepController.firstItem + 1}..#{stepController.lastItem} of #{stepController.itemCount}"/>&nbsp;
                    <h:commandLink action="#{stepController.prev}" value="Previous" rendered="#{stepController.firstItem >= stepController.batchSize}"/>&nbsp;
                    <h:commandLink action="#{stepController.next}" value="Next" rendered="#{stepController.lastItem < stepController.itemCount}"/>&nbsp;
                </div>               
                <h:dataTable value="#{stepController.steps}" var="item" columnClasses="stepname,steptype,stepformat,stepformat,enabled,actions">
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
