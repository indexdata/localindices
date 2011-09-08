<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" type="text/css" href="css/styles.css"/>
        <title>Local Unified Index</title>
    </head>
    <body>
        <f:view>
            <h:form>
                <h:commandLink value="Resources" action="#{resourceController.listResources}" />
                <br>
                <h:commandLink value="Storages"  action="#{storageController.listStorages}" />
                 <br>
                <h:commandLink value="Transformations" action="#{transformationController.list}" />
            </h:form>
        </f:view>
    </body>  
</html>