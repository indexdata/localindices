<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="edit_xsd_form">
    <div id="XSDditStep" style="display:inline">
        <h:panelGrid columns="1">
            <h:outputText value="Schema Validation (XSD):" />
            <h:inputTextarea
                value="#{stepController.current.script}"
                rows="30" cols="100" />
        </h:panelGrid>
    </div>  
</f:subview>