#!/usr/bin/ruby -w
# sudo aptitude install libdbi-ruby
# sudo aptitude install libdbd-mysql-ruby
#require 'rubygems'
require 'dbi'

commited_path = "/var/cache/harvested/committed"

begin
  dbh = DBI.connect('DBI:MySQL:localindices:localhost',
  'localidxadm', 'localidxadmpass')
  sth = dbh.prepare("SELECT ID, DTYPE, METADATAPREFIX FROM HARVESTABLE")
  sth.execute
  sth.fetch do |row|
    zdb_name = "job#{row[0]}"
    dom_conf = ""
    case row[1]
      when "OaiPmhResource" 
        if row[2] == "marc21"
          dom_conf = "oaimarc21-pz.xml"
        else
          dom_conf = "oaidc-pz.xml"
        end
      when "XmlBulkResource"
        dom_conf = "marc-pz.xml"
      when "WebCrawlResource" 
        dom_conf = "pz-pz.xml"
    end
    upd_stmt = 
      "zebraidx -tdom.#{dom_conf} -d #{zdb_name} -l #{zdb_name}.log update #{commited_path}/#{zdb_name}"
    print upd_stmt + "\n"
  end
  sth.finish
rescue DBI::DatabaseError => e
  puts "Error: #{e.message}"
ensure
  dbh.disconnect if dbh
end
