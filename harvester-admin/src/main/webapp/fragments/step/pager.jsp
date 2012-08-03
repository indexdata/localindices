<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="step_pager">
	<div id="pager">
		<h:outputText
			value="Item #{stepController.firstItem + 1}..#{stepController.lastItem} of #{stepController.itemCount}"
			rendered="#{stepController.itemCount > 0}" />
		&nbsp;
		<h:commandLink action="#{stepController.prev}"
			value="Previous #{stepController.batchSize}"
			rendered="#{stepController.firstItem > stepController.batchSize}" />
		&nbsp;
		<h:commandLink action="#{stepController.next}"
			value="Next #{stepController.batchSize}"
			rendered="#{stepController.lastItem + stepController.batchSize < stepController.itemCount}" />
		&nbsp;
	</div>
</f:subview>