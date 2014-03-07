#!/bin/bash

if [ "$1" == "" ] ; then 
    echo "$0: Generates an index file in current directory of all parameter (expanded in the shell). Does not support spaces"
fi

cat > index.html <<EOF
<html><body>
EOF

for d in $* ; do
    cat >> index.html <<EOF
  <a href="$d">$d</a>
EOF
done

cat >> index.html <<EOF
</body></html>
EOF
