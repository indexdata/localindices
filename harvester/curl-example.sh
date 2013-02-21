#!/bin/sh
C=id.cf
if test "$1"; then
	C=$1
fi
H=http://satay.index:9000/connector
# Create session (empty content)
curl -D ws.headers --output ws.log --data-binary "" $H
# Parse it
ID=`cat ws.log | cut -d":" -f 2|cut -d"}" -f 1`

# Load connector file
curl -D upload.headers --output upload.log --data-binary @$C $H/$ID/load_cf

# Run opt task init
curl -D init.headers --output init.log --header "Content-Type: application/json" --data-binary "{}" \
	$H/$ID/run_task_opt/init

curl -D harvest.headers --output harvest.log --header "Content-Type: application/json" --data-binary "{}" \
	$H/$ID/run_task/harvest

# Run task detailed
curl -D detail.headers --output detail.log --header "Content-Type: application/json" --data-binary "{ \"detailtoken\":\"https://www.indexdata.com/news/2013/01/index-data-introduces-connectors-cloud\"}" \
	$H/$ID/run_task/detail
# Take screen shot (requires pnmtopng, xwdtopnm)
if test -x /usr/bin/pnmtopng; then
	curl --output screen.png \
    	--data-binary "{}" 	$H/$ID/screen_shot
fi

RESUMPTIONTOKEN=`cat harvest.log |grep resumptiontoken | cut -d":" -f 2|cut -d"}" -f 1`
if [ "$RESUMPTIONTOKEN" != "" ] ; then
    echo $RESUMPTIONTOKEN
    curl --output harvest_resumption.log --header "Content-Type: application/json" \
	--data-binary "{\"resumptiontoken\":\"$RESUMPTIONTOKEN\" }" \
       $H/$ID/run_task/harvest
fi
# Get log
curl -D log.headers --output log.log --header "Content-Type: application/json" --data-binary "{}" \
	$H/$ID/log
# Get dom
curl -D dom.headers --output dom.log --header "Content-Type: text/html" --data-binary "{}" \
	$H/$ID/dom_string
# Delete the connector
curl --request DELETE $H/$ID

