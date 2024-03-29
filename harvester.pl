#!/usr/bin/perl

use Net::OAI::Harvester;

#'http://www.intechopen.com/oai/index.php';
my $url = @ARGV[0]; 

die "No url given" if (!$url);

$prefix = @ARGV[1];
die "No metadataPrefix given" if (!$prefix);

$set = '';
$set = @ARGV[2] if ($ARGV[2]);

$fromDate = '';
$fromDate = @ARGV[3] if ($ARGV[3]);

$untilDate = '';
$untilDate  = @ARGV[4] if ($ARGV[4]);


## create a harvester for the Library of Congress
my $harvester = Net::OAI::Harvester->new( 
    'baseURL' => $url
    );

my $identity = $harvester->identify();
print "name: ",$identity->repositoryName(),"\n";

## list all the records in a repository

my $records = $harvester->listRecords( metadataPrefix => $prefix, from => $fromDate, 'until' => $untilDate);
my $finished = 0;

while ( ! $finished ) {

    while ( my $record = $records->next() ) {
	my $header = $record->header();
	my $metadata = $record->metadata();
	print "Identifier: ",$header->identifier(),
	      ", Set: ", $header->sets(), 
	      ", Title: ",$metadata->title(), 
	      ", Url: ", $metadata->identifier(), "\n";
    }

    my $rToken = $records->resumptionToken();
    if ( $rToken ) { 
	print "resumptionToken: ", $rToken->token(), "\n";
	$records = $harvester->listRecords( 
	    resumptionToken => $rToken->token()
	    );

    } else { 
	$finished = 1;
    }
}
