#!/bin/bash

if [ "$1" == "" ] ; then 
    echo "$0: filepattern relocation" 
    echo "$0 *.xml ../../data" 
fi

echo "<html><body>" 
for d in $1 ; do 
    echo "<a href=\"$2$d\">$d</a>" ; 
done
echo "</body></html>"
