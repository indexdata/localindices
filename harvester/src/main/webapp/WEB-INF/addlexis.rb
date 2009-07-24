#!/usr/bin/ruby -w
# index lexis which is not specified as a harvesting job
#

commited_path = "/var/cache/harvested/committed"
conf_name = "zebra-reidx.cfg"

if argv.length < 1
  print "specify job number for lexis"
  exit
else
  job_name = "job#{argv[1]}"

upd_stmt = 
  "zebraidx -c #{conf_name} -tdom.dc-pz.xml -d #{job_name} -l #{job_name}.log update #{commited_path}/#{zdb_name}"
print upd_stmt + "\n"
system upd_stmt

print "\n"
print "zebraidx commit"
print "\n"

system "zebraidx -c #{conf_name} commit" 
