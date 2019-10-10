The dockerfile depends on the following environment variables:

MYSQLUSER - The username for the mysql db
MYSQLPASS - The password for the mysql db
MYSQLURL - The mysql url for the server. This must be at a location that the container can access.

Example:
export LOCAL_DOCKER_HOST=$(ip -4 addr show docker0 | grep -Po 'inet \K[\d.]+')
docker run -e MYSQLUSER='localidxadm' -e MYSQLPASS='localidxadmpass' -e MYSQLURL="jdbc:mysql://${LOCAL_DOCKER_HOST}:3306/localindices?autoReconnect=true" harvester

