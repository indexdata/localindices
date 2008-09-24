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
        <title>Login to use the service.</title>
    </head>
    <body>
        <f:view>
            <h3>Welcome to Localindices Admin</h3>
            <h:form>
                <h5><h:outputText value="Please login to use the service: " /></h5>
                <div id="pager">
                    <h:form>
                        <h:panelGrid columns="2">
                            <h:outputText value="Username:"/>
                            <h:inputText value="#{loginManager.username}" required="true"/>
                            <h:outputText value="Password:"/>
                            <h:inputSecret value="#{loginManager.password}" required="true"/>
                            <h:commandButton value="Login" action="#{loginManager.doLogin}"/>
                        </h:panelGrid>
                    </h:form>
                </div>
            </h:form>
        </f:view>
    </body>
</html>
