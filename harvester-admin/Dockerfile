FROM jetty

USER root
RUN mkdir -p /etc/masterkey/harvester
RUN mkdir -p /var/cache/harvester
RUN mkdir -p /var/log/masterkey/harvester
RUN chown jetty:jetty -R /var/log/masterkey
COPY target/harvester-admin /var/lib/jetty/webapps/harvester-admin
COPY docker_files/start.sh /usr/local/jetty/start.sh
RUN chown jetty:jetty /var/lib/jetty/webapps/harvester-admin/WEB-INF/web.xml
EXPOSE 8081
CMD ["sh", "/usr/local/jetty/start.sh"]
