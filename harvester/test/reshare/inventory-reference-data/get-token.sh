host=$1
tenant=$2
username=$3
password=$4
protocol=$5

curl -s -X POST -D - -H "Content-type: application/json" -H "X-Okapi-Tenant: $tenant"  -d "{ \"username\": \"$username\", \"password\": \"$password\"}" $protocol://$host/authn/login | grep x-okapi-token | cut -d " " -f2 

