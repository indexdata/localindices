
### Build
FROM maven:3.6.3-openjdk-8 as builder

#ARG LOCALINDICES_RELEASE_TAG

#RUN cd /usr/src && \
    #git clone https://github.com/indexdata/localindices && \
    #cd /usr/src/localindices && \
    #git checkout master
    #git checkout tags/${LOCALINDICES_RELEASE_TAG}


COPY . /usr/src

WORKDIR /usr/src
RUN mvn package

### harvester runtime container

FROM jetty:9.3-jre8 as masterkey-harvester
USER root
RUN mkdir -p /etc/masterkey/harvester && \
    mkdir -p /var/cache/harvester && \
    mkdir -p /var/log/masterkey/harvester && \
    chown -R jetty:jetty /var/log/masterkey/harvester && \
    chown -R jetty:jetty /var/cache/harvester 
COPY --from=builder harvester/target/harvester /var/lib/jetty/webapps/harvester
COPY --from=builder harvester/target/harvester/WEB-INF/stylesheets/ /var/lib/jetty/
COPY --from=builder harvester/docker_files/mysql-jetty-env.xml /var/lib/jetty/webapps/harvester/WEB-INF/jetty-env.xml
COPY --from=builder harvester/target/harvester/WEB-INF/harvester.properties /etc/masterkey/harvester/harvester.properties
#COPY files/harvester.properties /etc/masterkey/harvester/harvester.properties
COPY --from=builder harvester/test /var/lib/jetty/webapps/test
USER jetty
EXPOSE 8080
CMD ["java", "-jar", "/usr/local/jetty/start.jar"]

### harvester-admin runtime container

#FROM jetty:9.3-jre8 as masterkey-harvester-admin

#USER root
#RUN mkdir -p /etc/masterkey/harvester && \
#    mkdir -p /var/cache/harvester && \
#    mkdir -p /var/log/masterkey/harvester && \
#    chown jetty:jetty -R /var/log/masterkey
#COPY --from=builder /usr/src/localindices/harvester-admin/target/harvester-admin /var/lib/jetty/webapps/harvester-admin
#COPY --from=builder /usr/src/localindices/harvester-admin/docker_files/start.sh /usr/local/jetty/start.sh
#RUN chown -R jetty:jetty /var/lib/jetty/webapps/harvester-admin
#USER jetty
#EXPOSE 8081
#CMD ["sh", "/usr/local/jetty/start.sh"]

