
use Net::OAI::Harvester;


## create a harvester for the Library of Congress
my $harvester = Net::OAI::Harvester->new( 
    'baseURL' => 'http://oai.artstor.org/oaicatmuseum/OAIHandler'
    );

my $identity = $harvester->identify();
print "name: ",$identity->repositoryName(),"\n";

## list all the records in a repository

my $records = $harvester->listRecords( metadataPrefix => 'oai_dc', set => 'SetSSCDelaware,SET1' );
my $finished = 0;

while ( ! $finished ) {

    while ( my $record = $records->next() ) {
	my $header = $record->header();
	my $metadata = $record->metadata();
	print "Identifier: ",$header->identifier(),", Set: ", $header->sets(), ", Title: ",$metadata->title(), ", Url: ", $metadata->identifier(), "\n";
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


