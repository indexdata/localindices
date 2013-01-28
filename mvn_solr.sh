
# Start up Solr in the jetty container
java -jar start.jar > solr.log 2>&1 &
PID=$!
echo "Solr starting ($PID)" 
sleep 10
mvn -P integration-tests $*
kill $PID
