<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="edit_xsl_form">
    <div id="XSLeditStep" style="display:inline">
        <h:panelGrid columns="1">
            <h:outputText value="Transformation (XSL):" />
            <h:inputTextarea value="#{stepController.current.script}" rows="30" cols="100" />
        </h:panelGrid>
    </div>  
</f:subview>