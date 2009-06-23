#!/usr/bin/ruby -w
# sudo aptitude install libdbi-ruby libdbd-mysql-ruby
#
# NOTE - This *has* to be run as the glassfish user!
# Otherwise your file permissions will be totally wrong and zebra will die
#

require 'dbi'

commited_path = "/var/cache/harvested/committed"
conf_name = "zebra-reidx.cfg"

begin
  #print "asadmin stop-domain domain1 \n"
  #system "asadmin stop-domain domain1 "
  dbh = DBI.connect('DBI:MySQL:localindices:localhost',
  'localidxadm', 'localidxadmpass')

  print "\n"
  print "zebraidx init"
  print "\n"
  system "zebraidx -c #{conf_name} init"

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
      "zebraidx -c #{conf_name} -tdom.#{dom_conf} -d #{zdb_name} -l #{zdb_name}.log update #{commited_path}/#{zdb_name}"
    print upd_stmt + "\n"
    system upd_stmt
  end
  
  sth.finish
  print "\n"
  print "zebraidx commit"
  print "\n"
 
  system "zebraidx -c #{conf_name} commit"
  #print "asadmin start-domain domain1 \n"
  #system "asadmin start-domain domain1 "

rescue DBI::DatabaseError => e
  puts "Error: #{e.message}"
ensure
  dbh.disconnect if dbh
end
