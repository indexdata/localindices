# Install and run localindices in a Vagrant box

## Requirements:

* localindices (this project) checked out
* JDK 8
* Maven
* Ansible
* Vagrant

## Installation of the box

In root of localindices, mvn install the application, (i.e. with OpenJDK):

`export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 ; mvn clean install`

Then, with the help of Ansible (dev-deploy.yml) and Vagrant, install everything except for the database:

`vagrant up`

## Create database unless previous one can be used
And create the database from within the box <sup>[why]</sup>:

`vagrant ssh -c "/vagrant/sql/create-database-populate-tables.sh"`

## Otherwise, just restart tomcat<sup>[why2]</sup>

`vagrant ssh -c 'sudo service tomcat9 restart'`

## verify

Test the administration interface at

`http://localhost:8080/harvester-admin/`


<sup>[why]</sup> Due some issue with passing credentials to the creation of MySQL in Ansible, database creation is handled in a
script afterward.

<sup>[why2]</sup> Cannot make dev-deploy.yml restart it.
