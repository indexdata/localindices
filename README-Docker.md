### Building and running the Harvester and Harvester-Admin as Docker containers

Pre-built Docker images for the Masterkey Harvester and Harvester-Admin are available from
the Index Data Github Packages container registry.

```
docker pull ghcr.io/indexdata/harvester:master
docker pull ghrc.io/indexdata/harvester-admin:master
```

See https://github.com/indexdata/localindices/pkgs/container/harvester and 
https://github.com/indexdata/localindices/pkgs/container/harvester-admin for other
tags and releases options.


The docker images can also be built directly from this repository.

```
git clone https://github.com/indexdata/localindices
cd localindices
docker build -t harvester --target harvester . 
docker build -t harvester-admin --target harvester-admin .
```


### Running the Harvester container 

The Harvester container requires MySQL connection information which can
be passed to the container at runtime as the following environment variables.

* MYSQLUSER - The username for the MySQL DB
* MYSQLPASS - The password for the MySQL DB
* MYSQLURL  - The MySQL JDBC connection string to the MySQL database.

Example:

```
docker run -d --networks harvester -e MYSQLUSER='localidxadm' -e MYSQLPASS='localidxadmpass' -e MYSQLURL="jdbc:mysql://mysql:3306/localindices?autoReconnect=true" --name harvester harvester

```

### Running the Harvester Admin container

The Harvester Admin container requires the following environment variable:

* HARVESTER_HOST - The hostname ofthe Harvester container 

Example:

```
docker run -d --networks harvester -p 8081:8081 -e HARVESTER_HOST=harvester harvester-admin
```

