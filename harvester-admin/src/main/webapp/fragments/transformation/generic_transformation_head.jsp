<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" type="text/css" href="css/styles.css"/>
        <title>Transformation Pipeline </title>
        <script>
        function showEditStep() {
            document.getElementById('editStep').style.display='inline';
            document.getElementById('addStep').style.display='none';
            return 1;
        }    
        
        function hideEditStep() {
            document.getElementById('editStep').style.display='none';
            document.getElementById('addStep').style.display='inline';
            return 1;
        }    
        </script>
    </head>
