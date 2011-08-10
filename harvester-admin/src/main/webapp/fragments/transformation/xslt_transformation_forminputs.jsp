<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="xslt_form_inputs">
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
				<h:outputText value="Available Actions" />
			</f:facet>
			<h:commandLink value="Edit" action="#{transformationController.editStep}"
                onclick="return showEditStep();" >
                <f:param name="ID" value="#{item.id}" />
				<f:param name="stepID" value="#{item.id}" />
			</h:commandLink>
			<h:commandLink value="Up" action="#{transformationController.upStep}">
				<f:param name="stepID" value="#{item.id}" />
			</h:commandLink>
			<h:commandLink value="Down"
				action="#{transformationController.downStep}">
				<f:param name="stepID" value="#{item.id}" />
			</h:commandLink>
			<h:outputText value=" | " />
			<h:commandLink value="Delete"
				action="#{transformationController.deleteStep}"
				onclick="return confirm('Are you sure?');">
				<f:param name="stepID" value="#{item.id}" />
			</h:commandLink>
		</h:column>
	</h:dataTable>
    <div id="addStep" style="display:inline">  
           <h:outputText value=" Add: " />
           <h:commandLink value=" XslStep"
               action="#{transformationController.addXslStep}"
                onclick="return showEditStep();">
               <f:param name="stepID" value="#{item.id}" />
            </h:commandLink>
           <h:commandLink value=" Crosswalk"
               action="#{transformationController.addCrosswalkStep}"
                onclick="return showEditStep();">
               <f:param name="stepID" value="#{item.id}" />
            </h:commandLink>
           <h:commandLink value=" Validation Step"
               action="#{transformationController.addValidationStep}"
                onclick="return showEditStep();">
               <f:param name="stepID" value="#{item.id}" />
            </h:commandLink>
           <h:commandLink value=" Split Step"
               action="#{transformationController.addSplitStep}"
                onclick="return showEditStep();">
               <f:param name="stepID" value="#{item.id}" />
            </h:commandLink>
    </div>  
    <div id="editStep" style="display:inline">
        <h:panelGrid columns="2">
            <h:outputText value="Step Name:" />
            <h:inputText
                value="#{transformationController.transformationStep.name}" />
        </h:panelGrid>
		<h:panelGrid columns="1">
			<h:outputText value="XSLTransformation:" />
			<h:inputTextarea
				value="#{transformationController.transformationStep.script}"
				rows="40" cols="80" />
        </h:panelGrid>
        <h:outputText value=" Edit Step Action: " />
        <h:commandLink value=" Save"
            action="#{transformationController.saveStep}"
            onclick="return hideEditStep();">
            <f:param name="stepID" value="#{item.id}" />
        </h:commandLink>
        <h:commandLink value=" Cancel"
            action="#{transformationController.cancelStep}"
            onclick="return hideEditStep();">
            <f:param name="stepID" value="#{item.id}" />
        </h:commandLink>
    </div>  
</f:subview>