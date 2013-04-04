<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="edit_xsl_form">
    <div id="XSLeditStep" style="display:inline">
        <h:panelGrid columns="1">
        <h:outputText value="Custom Step Class:" />
        <!--  Nice to list all available classes, but they are not present in the Admin (yet) -->
        <h:inputText value="#{stepController.current.customClass}" />
        </h:panelGrid>
    </div>  
</f:subview>