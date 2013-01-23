#
if [ -f "$1" ] ; then
    mysql -ulocalidxadm -plocalidxadmpass localindices < $1
else
    echo "No such script: $1"
fi
#

