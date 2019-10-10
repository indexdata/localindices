#!/bin/sh
sed -i "s/localhost/${HARVESTER_HOST}/g" /var/lib/jetty/webapps/harvester-admin/WEB-INF/web.xml
java -jar /usr/local/jetty/start.jar -Djetty.http.port=8081
