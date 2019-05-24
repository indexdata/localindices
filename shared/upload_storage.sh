#!/bin/sh

curl -i -w "\n" -X POST -H "Content-Type:application/xml" -d @inventorystorageupload.xml "http://localhost:8080/harvester/records/storages"

