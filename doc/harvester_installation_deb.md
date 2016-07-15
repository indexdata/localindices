The following document describes the installing the Masterkey Harvester v2.11 
and Masterkey Local Unified Index (LUI) v0.9 on a Debian 8 system.


Pre-Installation
================

*  Add the Index Data apt repository to your system.   Add the following content to
/etc/apt/sources.list.d/indexdata.list:

```
     deb http://ftp.indexdata.dk/debian jessie main 
     deb-src http://ftp.indexdata.dk/debian jessie main
     deb http://ftp.indexdata.dk/debian jessie restricted 
     deb-src http://ftp.indexdata.dk/debian jessie restricted

```

*  The 'restricted' repo requires IP authentication.   Please send access requests to 'support@indexdata.com' if necessary. 

*  Index Data packages are signed with a GPG key.   Add the key to your apt keyring.

```
     wget http://ftp.indexdata.com/pub/debian/indexdata.asc | sudo apt-key add -
     sudo apt-get update

```

*  Masterkey LUI stores its Solr indexes by default in /var/lib/masterkey/lui.
Ensure this partition is large enough to accomodate your Solr indexes.


Installation
============

The Masterkey Harvester essentially consists of the Harvester application which runs
as a Tomcat servlet and MySQL database.   The Masterkey Local Unified Index is 
Solr bundled with the necessary configs and Tomcat.  They can be installed on 
separate nodes if desired. 


Masterkey LUI Installation
--------------------------


*  Install the following packages:

```
     sudo apt-get install masterkey-lui-solr4-common masterkey-lui-solr4-common-tomcat8 \
                          masterkey-lui-solr4-master masterkey-lui-solr4-master-tomcat8

```

*  Increase the max JVM heap size for Tomcat.  The ideal setting depends on several
factors, including the total amount of RAM available on the system and the size of your
indexes.   The Debian default of 128m, however, is much too low.  Edit 
/etc/default/tomcat8 and increase '-Xmx'.   Example:

```
     JAVA_OPTS="-Djava.awt.headless=true -Xmx2048m -XX:+UseConcMarkSweepGC"

```

*  Restart Tomcat.

*  Verify that Solr4 is running normally by connecting to the Solr Admin console 
on port 8080:

```
     curl http://localhost:8080/solr4

```

*  Solr logs are available at /var/log/tomcat8/solr.log.  Also check /var/log/tomcat8/catalina.out for additional Tomcat log output. 


Masterkey Harvester Installation
--------------------------------

*  Install the following packages:

```
     sudo apt-get install masterkey-harvester-admin masterkey-harvester-admin-tomcat8 \
                          masterkey-harvester-engine masterkey-harvester-engine-tomcat8 \
                          masterkey-harvester-mysql masterkey-harvester-utils

```

*  Create the Harvester MySQL database and user:

```
     sudo mysql -u root -p 
     MYSQL>  create database localindices; 
     MYSQL>  grant all privileges on localindices.* to 'localidxadm'@'localhost' identified by 'localidxadmpass';

```

Note: 'localidxadmpass' is the default DB password.  On production systems, you should 
change the password and update the following Harvester configuration files:

     /etc/masterkey/harvester/harvester-context.xml
     /etc/masterkey/harvester-admin/harvester-admin-context.xml

*  Apply Harvester schema and apply sql patches to database

```
     mysql -u localidxadm -p localindices < /usr/share/masterkey/harvester/sql/schema.v2.8.sql
     mysql -u localidxadm -p localindices < /usr/share/masterkey/harvester/sql/v2.9/2016-05-03.sql
     mysql -u localidxadm -p localindices < /usr/share/masterkey/harvester/sql/v2.10/2016-07-04.sql
     mysql -u localidxadm -p localindices < /usr/share/masterkey/harvester/sql/v2.11/2016-07-15.sql

```

*  Verify that harvester admin console is available. 

```
     curl http://localhost:8080/harvester-admin/

```

*  Harvester logs are available in the /var/log/masterkey directory. 


