### Build
FROM maven:3.6.3-openjdk-8 as builder

COPY . /usr/src

WORKDIR /usr/src
RUN mvn clean package

### harvester runtime image

FROM jetty:9.4-jre8 as harvester
USER root
RUN mkdir -p /etc/masterkey/harvester && \
    mkdir -p /var/cache/harvester && \
    mkdir -p /var/log/masterkey/harvester && \
    chown -R jetty:jetty /var/log/masterkey/harvester && \
    chown -R jetty:jetty /var/cache/harvester 
COPY --from=builder /usr/src/harvester/target/harvester /var/lib/jetty/webapps/harvester
COPY --from=builder /usr/src/harvester/target/harvester/WEB-INF/stylesheets/ /var/lib/jetty/
COPY --from=builder /usr/src/harvester/docker_files/mysql-jetty-env.xml /var/lib/jetty/webapps/harvester/WEB-INF/jetty-env.xml
COPY --from=builder /usr/src/harvester/target/harvester/WEB-INF/harvester.properties /etc/masterkey/harvester/harvester.properties
COPY --from=builder /usr/src/harvester/test /var/lib/jetty/webapps/test
USER jetty
EXPOSE 8080
CMD ["java", "-jar", "/usr/local/jetty/start.jar"]

### harvester-admin runtime image

FROM jetty:9.4-jre8 as harvester-admin

USER root
RUN mkdir -p /etc/masterkey/harvester && \
    mkdir -p /var/cache/harvester && \
    mkdir -p /var/log/masterkey/harvester && \
    chown jetty:jetty -R /var/log/masterkey
COPY --from=builder /usr/src/harvester-admin/target/harvester-admin /var/lib/jetty/webapps/harvester-admin
COPY --from=builder /usr/src/harvester-admin/docker_files/start.sh /usr/local/jetty/start.sh
RUN chown -R jetty:jetty /var/lib/jetty/webapps/harvester-admin
USER jetty
EXPOSE 8081
CMD ["sh", "/usr/local/jetty/start.sh"]

