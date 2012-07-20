<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="step_form_inputs">
	<h5>Transformation Steps:</h5>
	<h:dataTable value="#{transformationController.transformationSteps}"
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
				<h:outputText value="Actions" />
			</f:facet>
			<h:commandLink 
			    styleClass="action"  
			    action="#{transformationController.upStep}">
				<f:param name="stepID" value="#{item.id}" />
                <h:graphicImage title="Up" alt="Up" height="16" url="/images/up.png" />
			</h:commandLink>
			<h:commandLink styleClass="action" 
				action="#{transformationController.downStep}">
				<f:param name="stepID" value="#{item.id}" />
                <h:graphicImage title="Down" alt="Down" height="16" url="/images/down.png" />
			</h:commandLink>
			<h:commandLink styleClass="action"  
				action="#{transformationController.deleteStep}"
				onclick="return confirm('Are you sure?');">
				<f:param name="stepID" value="#{item.id}" />
                <h:graphicImage title="Delete" alt="Delete" height="16" url="/images/delete.png" />
			</h:commandLink>
		</h:column>
	</h:dataTable>
    <div id="addStep" style="display:inline">  
        <h:commandLink value="Add new Step" action="#{transformationController.selectStepToInsert}" />
    </div>
</f:subview>