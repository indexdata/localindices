<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="test_form">
    <div id="XSLeditStep" style="display:inline">
        <h:panelGrid columns="1">
            <h:outputText value="Test data:" />
            <h:inputTextarea value="" rows="30" cols="100" />
            <h:outputText value="Expected Output:" />
            <h:inputTextarea value="" rows="30" cols="100" />
            <h:commandLink value="Test" action="#{stepController.test}">
                <f:param name="id" value="#{item.id}" />
            </h:commandLink>
        </h:panelGrid>
    </div>  
</f:subview>