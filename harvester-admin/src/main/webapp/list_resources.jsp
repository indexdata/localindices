<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="stylesheet" type="text/css" href="css/styles.css" />
<title>Resources</title>
</head>
<body>
	<f:view>
            <h:outputText style="h1">Resources</h:outputText>
    		<h:form>
            <%@ include file="fragments/navigation_panel.jsp" %>
			<br>
			<h:outputText value="Add new resource: " />
			<h:commandLink styleClass="navigation" value="OAI-PMH"
				action="#{resourceController.prepareOaiPmhResourceToAdd}" />
			<!-- 
                 <h:outputText value=", "/>
                <h:commandLink styleClass="navigation" value="WebCrawl" action="#{resourceController.prepareWebCrawlResourceToAdd}" />       
 -->
			<h:outputText value=", " />
			<h:commandLink styleClass="navigation" value="XML/MARC bulk"
				action="#{resourceController.prepareXmlBulkResourceToAdd}" />
			<!-- 
                <h:outputText value=", "/>
 -->
		</h:form>
		<h:form>
			<h3>
				<h:outputText value="Available resources:" />
			</h3>
			<div id="pager">
				<h:outputText
					value="Item #{resourceController.firstItem + 1}..#{resourceController.lastItem} of #{resourceController.itemCount}" />
				&nbsp;
				<h:commandLink action="#{resourceController.prev}"
					value="Previous #{resourceController.batchSize}"
					rendered="#{resourceController.firstItem >= resourceController.batchSize}" />
				&nbsp;
				<h:commandLink action="#{resourceController.next}"
					value="Next #{resourceController.batchSize}"
					rendered="#{resourceController.lastItem + resourceController.batchSize <= resourceController.itemCount}" />
				&nbsp;
				<h:commandLink action="#{resourceController.next}"
					value="Remaining #{resourceController.itemCount - resourceController.lastItem}"
					rendered="#{resourceController.lastItem < resourceController.itemCount && resourceController.lastItem + resourceController.batchSize > resourceController.itemCount}" />
			</div>
			<h:dataTable width="100%" value="#{resourceController.resources}"
				var="item"
				columnClasses="resourcename,status,lastharvest,nextharvest,actions,statusmessage">
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
						<h:outputText value="Last Harvested" />
					</f:facet>
					<h:outputText rendered="#{item.lastHarvestFinished != null}"
						value="#{item.lastHarvestFinished}">
						<f:convertDateTime pattern="yyyy-MM-dd HH:mm z" />
					</h:outputText>
					<h:outputText styleClass="attempt"
						rendered="#{item.lastHarvestFinished == null}"
						value="#{item.lastHarvestStarted}">
						<f:convertDateTime pattern="yyyy-MM-dd HH:mm z" />
					</h:outputText>
				</h:column>
				<h:column>
					<f:facet name="header">
						<h:outputText value="Next Scheduled Harvest" />
					</f:facet>
                    <h:outputText rendered="#{item.enabled}"
					   value="#{item.nextHarvestSchedule}">
						<f:convertDateTime pattern="yyyy-MM-dd HH:mm z" />
					</h:outputText>
                    <h:outputText rendered="#{!item.enabled }"
                       value="Disabled">
                    </h:outputText>
				</h:column>
				<h:column>
					<f:facet name="header">
						<h:outputText value="Actions" />
					</f:facet>
					<h:commandLink styleClass="action"
						action="#{resourceController.prepareResourceToEdit}">
						<f:param name="resourceId" value="#{item.id}" />
						<h:graphicImage title="Edit" alt="Edit" height="16" url="/images/edit.png" />
					</h:commandLink>
					<h:commandLink styleClass="action"
						action="#{resourceController.prepareResourceToRun}">
						<f:param name="resourceId" value="#{item.id}" />
						<h:graphicImage title="Run" alt="Run" height="16" url="/images/run.png" />
					</h:commandLink>
					<h:commandLink styleClass="action"
						action="#{resourceController.deleteResource}"
						onclick="return confirm('Are you sure?');">
						<f:param name="resourceId" value="#{item.id}" />
						<h:graphicImage title="Delete" alt="Delete" height="16" url="/images/delete.png" />
					</h:commandLink>
					<h:commandLink styleClass="action"
						action="#{resourceController.viewJobLog}">
						<f:param name="resourceId" value="#{item.id}" />
						<h:graphicImage title="View Log" alt="View Log" height="16" url="/images/log.png" />
					</h:commandLink>
				</h:column>
				<h:column>
					<f:facet name="header">
						<h:outputText value="Status Message" />
					</f:facet>
					<h:outputText value="#{item.message}"></h:outputText>
				</h:column>
			</h:dataTable>
		</h:form>
	</f:view>
</body>
</html>
