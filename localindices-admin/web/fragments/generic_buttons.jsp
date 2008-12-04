<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="generic_edit_buttons">
    <h5>Actions: </h5>
    <h:commandButton value="Save Changes & Exit" action="#{resourceController.saveResource}"/>
    <h:commandButton value="Cancel Changes & Exit" action="list_resources"/>
    <h:commandButton onclick="return confirm('Are you sure?');" value="Delete Harvested Records" action="#{resourceController.saveAndPurge}"/>
    <br/>
    <h5>Status Info: </h5>
    <h:outputText styleClass="item" value="Initially harvested -- #{resourceController.resource.initiallyHarvested}"/>
    <br/>
    <h:outputText styleClass="item" value="Start time of last harvest -- #{resourceController.resource.lastHarvestStarted}"/>
    <br/>
    <h:outputText styleClass="item" value="Finish time of last harvest -- #{resourceController.resource.lastHarvestFinished}"/> 
    <br/>
    <div class="footer">
        <h:outputText value="Last Updated -- #{resourceController.resource.lastUpdated}"/>
    </div>
</f:subview>
