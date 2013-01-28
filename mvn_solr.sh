
# Start up Solr in the jetty container
java -jar start.jar & 
PID=$$
mvn $*
kill $PID