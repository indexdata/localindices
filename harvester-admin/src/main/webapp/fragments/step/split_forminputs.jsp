<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<f:subview id="edit_xsl_form">
    <div id="editStep" style="display:inline">
        <h:panelGrid columns="2">
            <h:outputText value="Split at depth (Zero disables splitting):"/>
            <h:inputText value="#{stepController.splitAt}"/>
            <h:outputText value="Split (number of records. Zero or Empty disables split):"/>
            <h:inputText value="#{stepController.splitSize}"/>
        </h:panelGrid>
    </div>  
</f:subview>