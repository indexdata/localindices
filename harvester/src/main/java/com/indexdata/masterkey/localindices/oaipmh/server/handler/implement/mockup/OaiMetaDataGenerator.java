package com.indexdata.masterkey.localindices.oaipmh.server.handler.implement.mockup;

import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhRequest;

public class OaiMetaDataGenerator {
  
  OaiPmhRequest request;

  String recordStart = 
      "		<record>\n"; 
  String recordEnd =
      "		</record>\n"; 
  
  String recordHeader = 
      	"			<header>\n" + 
	  "				<identifier>oai:ojs.ijict.org:article/156</identifier>\n" + 
	  "				<datestamp>2010-10-11T05:39:24Z</datestamp>\n" + 
	  "				<setSpec>ijoat:EA</setSpec>\n" + 
	  "			</header>\n";

  String metaData = 
      "			<metadata>\n";
  String metaDataEnd = 
      "			</metadata>\n";
  
  String oai_dc = 
      "				<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" + 
      "					xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
      "					xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/\n" + 
      "    				http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" +
      "					<dc:title xml:lang=\"en-US\">Advancements in Technology: What’s in beholding for Human Beings?</dc:title>\n" + 
      "					<dc:creator>S.S. Iyengar; Louisiana State University</dc:creator>\n" + 
      "					<dc:subject xml:lang=\"en-US\">Communication Systems</dc:subject>\n" + 
      "					<dc:subject xml:lang=\"en-US\">wireless sensor network, virtual reality, information theory, soft computing, data engineering and architecture</dc:subject>\n" + 
      "					<dc:subject xml:lang=\"en-US\">Advancements in Technology</dc:subject>\n" + 
      "					<dc:description xml:lang=\"en-US\">Science fictions of yesteryears are a reality today. We are now living in a unbelievable tech-age. Technology has enabled human beings to fit visual sensing devices on a fly, make hybrid car capable of running on gas as well as electricity, simulate black hole and eventual evolution of Universe using Large Hadron Collider, and BloomEnergy devising Energy Server of the size of bread loaf; capable of satisfying all energy needs at Fortune 500 incorporations like Google, Staples, Bank of America, eBay, Staples, Walmart and more. The digital revolution has even electronic manufacturers competing neck to neck with each other. What&amp;rsquo;s fastest and latest today, takes no time in becoming history the very next day.</dc:description>\n" + 
      "					<dc:publisher xml:lang=\"en-US\">International Journal of Advancements in Technology</dc:publisher>\n" + 
      "					<dc:contributor xml:lang=\"en-US\"></dc:contributor>\n" + 
      "					<dc:date>2010-10-14</dc:date>\n" + 
      "					<dc:type xml:lang=\"en-US\"></dc:type>\n" + 
      "					<dc:type xml:lang=\"en-US\"></dc:type>\n" + 
      "					<dc:format>application/pdf</dc:format>\n" + 
      "					<dc:identifier>http://ijict.org/index.php/ijoat/article/view/advancements-in-technology</dc:identifier>\n" + 
      "					<dc:source xml:lang=\"en-US\">International Journal of Advancements in Technology; Vol 1, No 2 (2010): International Journal of Advancements in Technology Vol 1 No 2; 163-165</dc:source>\n" + 
      "					<dc:language>en</dc:language>\n" + 
      "					<dc:coverage xml:lang=\"en-US\"></dc:coverage>\n" + 
      "					<dc:coverage xml:lang=\"en-US\"></dc:coverage>\n" + 
      "					<dc:coverage xml:lang=\"en-US\"></dc:coverage>\n" + 
      "					<dc:rights>Authors who publish with this journal agree to the following terms:&lt;br /&gt; &lt;ol type=&quot;a&quot;&gt;&lt;br /&gt;&lt;li&gt;Authors retain copyright and grant the journal right of first publication with the work simultaneously licensed under a &lt;a href=&quot;http://creativecommons.org/licenses/by/3.0/&quot; target=&quot;_new&quot;&gt;Creative Commons Attribution License&lt;/a&gt; that allows others to share the work with an acknowledgement of the work's authorship and initial publication in this journal.&lt;/li&gt;&lt;br /&gt;&lt;li&gt;Authors are able to enter into separate, additional contractual arrangements for the non-exclusive distribution of the journal's published version of the work (e.g., post it to an institutional repository or publish it in a book), with an acknowledgement of its initial publication in this journal.&lt;/li&gt;&lt;br /&gt;&lt;li&gt;Authors are permitted and encouraged to post their work online (e.g., in institutional repositories or on their website) prior to and during the submission process, as it can lead to productive exchanges, as well as earlier and greater citation of published work (See &lt;a href=&quot;http://opcit.eprints.org/oacitation-biblio.html&quot; target=&quot;_new&quot;&gt;The Effect of Open Access&lt;/a&gt;).&lt;/li&gt;&lt;/ol&gt;</dc:rights>\n" + 
      "				</oai_dc:dc>\n"; 

  public OaiMetaDataGenerator(OaiPmhRequest request) {
    this.request = request;
  }
  
  public void generateRecords(StringBuffer xml) {
    String prefix = request.getParameter("metadataPrefix");
    if (!"oai_dc".equals(prefix))
      	throw new RuntimeException("Unsupported metadataPrefix: " + prefix);
    String set = request.getParameter("metadataPrefix");
    if (set != null) {
      // Parse setting from set.
    }
    int total = 2;
    for (int index = 0; index < total; index++) {
      generateRecord(xml);
    }
  }

  public void generateRecord(StringBuffer xml) {
    xml.append(recordStart)
	.append(recordHeader)
	.append(metaData).append(oai_dc).append(metaDataEnd)
	.append(recordEnd);
  }

}
