Given that localindices is mvn installed, this should install a Harvester in Ubuntu virtual box:

## Install everything except for the database: 

vagrant up 

## Then create the database from within the box:

vagrant ssh -c "/vagrant/sql/create-database-populate-tables.sh"

