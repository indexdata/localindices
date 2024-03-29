SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

sudo mysql -e "CREATE DATABASE localindices;"
sudo mysql -e "CREATE USER 'localidxadm'@'localhost' IDENTIFIED BY 'localidxadmpass';"
sudo mysql -e "GRANT ALL PRIVILEGES ON localindices.* TO 'localidxadm'@'localhost';"
export MYSQL_PWD=localidxadmpass
# Create tables and sample data
mysql -u localidxadm localindices <"$SCRIPT_DIR"/schema.v2.8-with-sample-data.sql
mysql -u localidxadm localindices <"$SCRIPT_DIR"/v2.9/2016-05-03.sql
mysql -u localidxadm localindices <"$SCRIPT_DIR"/v2.10/2016-07-04.sql
mysql -u localidxadm localindices <"$SCRIPT_DIR"/v2.11/2016-07-15.sql
mysql -u localidxadm localindices<"$SCRIPT_DIR"/v2.11/v2.11-data.sql
mysql -u localidxadm localindices <"$SCRIPT_DIR"/v2.12/2020-04-01.sql
mysql -u localidxadm localindices <"$SCRIPT_DIR"/v2.13/2020-04-15.sql
mysql -u localidxadm localindices <"$SCRIPT_DIR"/v2.14/2020-04-14.sql

sudo service tomcat9 restart
