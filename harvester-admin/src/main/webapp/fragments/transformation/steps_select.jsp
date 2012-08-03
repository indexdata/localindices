<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="transformation_steps_select">
	<h5>Transformation Steps:</h5>
    <%@ include file="/fragments/step/pager.jsp" %>
	<h:dataTable value="#{stepController.steps}"
		var="item"
		columnClasses="right,left,center,left,center,center,center,center">
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
                <h:outputText value="In" />
            </f:facet>
            <h:outputText value="#{item.inputFormat}"></h:outputText>
        </h:column>
        <h:column>
            <f:facet name="header">
                <h:outputText value="Out" />
            </f:facet>
            <h:outputText value="#{item.outputFormat}"></h:outputText>
        </h:column>
		<h:column>
			<f:facet name="header">
				<h:outputText value="Actions" />
			</f:facet>
   			<h:commandLink 
			    styleClass="action"  
			    action="#{transformationController.addStep}">
                <f:param name="transformationID" value="#{transformationController.transformation.id}" />
				<f:param name="stepID" value="#{item.id}" />
                <h:graphicImage title="Add" alt="Add" height="16" url="/images/add.png" />
			</h:commandLink>
		</h:column>
	</h:dataTable>
    <div id="transformation_steps_select_close" style="display:inline">  
        <h:commandButton value="Close" action="#{transformationController.editCurrent}" />
    </div>
</f:subview>