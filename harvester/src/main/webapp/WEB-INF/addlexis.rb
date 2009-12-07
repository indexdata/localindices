#!/usr/bin/ruby -w
# index lexis which is not specified as a harvesting job
#

commited_path = "/var/cache/harvested/committed"
conf_name = "zebra.cfg"

if ARGV.length < 1
  print "specify job number for lexis\n"
  exit
else
  job_name = "job#{ARGV[0]}"
end

system "zebraidx -c #{conf_name} drop #{job_name}" 
upd_stmt = 
  "zebraidx -c #{conf_name} -tdom.dc-pz.xml -d #{job_name} -l #{job_name}.log update #{commited_path}/#{job_name}"
print upd_stmt + "\n"
system upd_stmt

print "\n"
print "zebraidx commit"
print "\n"

system "zebraidx -c #{conf_name} commit" 
