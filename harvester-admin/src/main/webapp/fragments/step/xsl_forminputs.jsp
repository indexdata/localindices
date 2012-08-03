<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="Not_used">
    <div id="addStep" style="display:inline">  
           <h:outputText value=" Add: " />
           <h:commandLink value=" Step"
               action="#{transformationController.addStep}"
                onclick="return showEditStep();">
               <f:param name="stepID" value="#{item.id}" />
            </h:commandLink>
    </div>  
    <div id="allsteps" style="display:inline">
    <h:dataTable value="#{stepController.list}"
        var="step"
        columnClasses="right,left,center,left,center,center,center,center">
        <h:column>
            <f:facet name="header">
                <h:outputText value="ID" />
            </f:facet>
            <h:outputText value="#{step.id}"></h:outputText>
        </h:column>
        <h:column>
            <f:facet name="header">
                <h:outputText value="Name" />
            </f:facet>
            <h:outputText value="#{step.name}"></h:outputText>
        </h:column>
        <h:column>
            <f:facet name="header">
                <h:outputText value="Action" />
            </f:facet>
            <h:commandLink 
                styleClass="action"  
                action="#{transformationController.addStep}">
                <f:param name="ID" value="#{item.id}" />
                <f:param name="stepID" value="#{step.id}" />
                <h:graphicImage title="Add" alt="Add" height="16" url="/images/add.png" />
            </h:commandLink>
        </h:column>
    </h:dataTable>
        
    </div>
</f:subview>