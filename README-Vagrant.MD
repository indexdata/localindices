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

And create the database from within the box <sup>[why]</sup>:

`vagrant ssh -c "/vagrant/sql/create-database-populate-tables.sh"`

Test the administration interface at

`http://localhost:8080/harvester-admin`


<sup>[why] Due some issue with passing credentials to the creation of MySQL in Ansible, database creation is handled in a
script afterwards.</sup>
