#/!bin/bash
set -f

if [ "$1" == "" ] ; then 
    echo "$0: Generates an index file of the directory structure. Patterns must be quote in order to avoid shell expansion."
    echo "$0 '*.html' \"*.xml\" ";  
    exit 1
fi
cat <<EOF
<html>
  <body>
EOF
for d in $* ; do
    find . ${FIND_OPTIONS} -name "${d}" | while read filename
    do
	cat <<EOF
    <a href="$filename">$filename</a>
EOF
    done
done
    cat <<EOF  
  </body>
</html>
EOF
