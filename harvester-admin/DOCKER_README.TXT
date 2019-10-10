Required environment variables:

HARVESTER_HOST - The location for where the instance of the harvester backend API can be located. This must be an IP accessible to the container.

Example:
export LOCAL_DOCKER_HOST=$(ip -4 addr show docker0 | grep -Po 'inet \K[\d.]+')
docker run -p 8081:8081 -e 'HARVESTER_HOST'=$LOCAL_DOCKER_HOST harvester-admin

