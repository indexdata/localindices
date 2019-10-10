FROM jetty

USER root
RUN mkdir -p /etc/masterkey/harvester
RUN mkdir -p /var/cache/harvester
RUN mkdir -p /var/log/masterkey/harvester
RUN chown -R jetty:jetty /var/log/masterkey/harvester
RUN chown -R jetty:jetty /var/cache/harvester
COPY target/harvester /var/lib/jetty/webapps/harvester
RUN rm /var/lib/jetty/webapps/harvester/WEB-INF/jetty-env.xml
COPY docker_files/mysql-jetty-env.xml /var/lib/jetty/webapps/harvester/WEB-INF/jetty-env.xml
COPY target/harvester/WEB-INF/harvester.properties /etc/masterkey/harvester/harvester.properties
USER jetty
EXPOSE 8080

CMD ["java", "-jar", "/usr/local/jetty/start.jar"]
