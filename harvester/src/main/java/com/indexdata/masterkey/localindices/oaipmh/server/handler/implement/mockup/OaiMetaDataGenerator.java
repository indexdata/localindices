package com.indexdata.masterkey.localindices.oaipmh.server.handler.implement.mockup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.oaipmh.server.handler.OaiPmhRequest;

public class OaiMetaDataGenerator {

  int index = 1;
  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
  SimpleDateFormat longDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
  // TODO For now there will be one document for each day. 
  long  step = 24 * 3600 * 1000; 
  int bulkSize = 100;
  int count = 0;
  Date from =  new Date();
  Date until   =  new Date();
  Date next   = new Date(from.getTime());
  // TODO This hack only works for steps of size day and only for short date format 
  Date realEnd  = new Date(until.getTime() + step);
  OaiPmhRequest request;
  // One day
  private String set = "";
  Map<String, Object> otherProperties;
  boolean more;

  String recordStart = 
      "		<record>\n"; 
  String recordEnd =
      "		</record>\n"; 
  
  String metaData = 
      "			<metadata>\n";
  String metaDataEnd = 
      "			</metadata>\n";
  
  String oai_dcStart = 
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
      "					<dc:contributor xml:lang=\"en-US\"></dc:contributor>\n"; 
  String oai_dcEnd = 

      "					<dc:type xml:lang=\"en-US\"></dc:type>\n" + 
      "					<dc:format>application/pdf</dc:format>\n" + 
      "					<dc:identifier>http://ijict.org/index.php/ijoat/article/view/advancements-in-technology</dc:identifier>\n" + 
      "					<dc:source xml:lang=\"en-US\">International Journal of Advancements in Technology; Vol 1, No 2 (2010): International Journal of Advancements in Technology Vol 1 No 2; 163-165</dc:source>\n" + 
      "					<dc:language>en</dc:language>\n" + 
      "					<dc:coverage xml:lang=\"en-US\"></dc:coverage>\n" + 
      "					<dc:rights>Authors who publish with this journal agree to the following terms:&lt;br /&gt; &lt;ol type=&quot;a&quot;&gt;&lt;br /&gt;&lt;li&gt;Authors retain copyright and grant the journal right of first publication with the work simultaneously licensed under a &lt;a href=&quot;http://creativecommons.org/licenses/by/3.0/&quot; target=&quot;_new&quot;&gt;Creative Commons Attribution License&lt;/a&gt; that allows others to share the work with an acknowledgement of the work's authorship and initial publication in this journal.&lt;/li&gt;&lt;br /&gt;&lt;li&gt;Authors are able to enter into separate, additional contractual arrangements for the non-exclusive distribution of the journal's published version of the work (e.g., post it to an institutional repository or publish it in a book), with an acknowledgement of its initial publication in this journal.&lt;/li&gt;&lt;br /&gt;&lt;li&gt;Authors are permitted and encouraged to post their work online (e.g., in institutional repositories or on their website) prior to and during the submission process, as it can lead to productive exchanges, as well as earlier and greater citation of published work (See &lt;a href=&quot;http://opcit.eprints.org/oacitation-biblio.html&quot; target=&quot;_new&quot;&gt;The Effect of Open Access&lt;/a&gt;).&lt;/li&gt;&lt;/ol&gt;</dc:rights>\n" + 
      "				</oai_dc:dc>\n";
  private String prefix;
  private boolean recordMode = true; 

  public OaiMetaDataGenerator(OaiPmhRequest request) {
    this.request = request;
    from  = parseDate(request.getParameter("from"), 0);
    until    = parseDate(request.getParameter("until"), new Date().getTime());
    count  = 0;
    prefix = request.getParameter("metadataPrefix");
    set    = request.getParameter("set");
    parseResumptionToken();

  }

  public OaiMetaDataGenerator(Date fromDate, Date untilDate, int seconds, Map<String, Object> properties) {
    from  = fromDate;
    until  = untilDate;
    this.step = seconds;
    otherProperties = properties;
    more = from.before(until);
  }

  public String generateRecords(StringBuffer xml) {
    if (!"oai_dc".equals(prefix))
    	throw new RuntimeException("Unsupported metadataPrefix: " + prefix);
    next = new Date(from.getTime() + (count * step));
    realEnd  = new Date(until.getTime() + step);
    //TODO side effect in generateRecord of printing out a record. Must be called after count < bulkSize 
    int index = 0;
    while (index < bulkSize && generateRecord(xml)) {
      count++;
      index++;
      next = new Date(from.getTime() + (count * step));
    }
    
    return generateResumptionToken(); 
  }

  public void parseResumptionToken() {
    String resumptionToken = request.getParameter("resumptionToken");
    if (resumptionToken != null) {
      String[] parameters = resumptionToken.split("\\|");
      if (parameters.length >= 5) {
	prefix = parameters[0];
	from = parseDate(parameters[1], 0l);
        until = parseDate(parameters[2], new Date().getTime());
        set = (parameters[3].equals("") ? null : parameters[3]); 
        try {
          count = Integer.parseInt(parameters[4]);
        } catch (Exception ex) {
          Logger.getLogger(this.getClass()).warn("Failed to parse count on resumption token: " + resumptionToken);
          throw new RuntimeException("<error code=\"badResumptionToken\">Invalid resumption token '" + resumptionToken + "'</error>");
        }
      }
      else {
	throw new RuntimeException("<error code=\"badResumptionToken\">Invalid resumption token '" + resumptionToken + "'</error>");
      }
    }
  }

  private Date parseDate(String stringRep, long time) 
  {
    Date date = new Date(time);
    if (stringRep == null || "".equals(stringRep))
      return date;
    try {
      return dateFormat.parse(stringRep);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    try {
      return longDateFormat.parse(stringRep);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    try {
      long newTime = Long.parseLong(stringRep);
      date = new Date(newTime);
      return date;
    } catch (Exception e) {
      e.printStackTrace();
    }
    //TODO Throw error. 
    return new Date(time);
  }

  private String generateResumptionToken() {
    if (more) {
      StringBuffer token = new StringBuffer("");
      token.append(prefix).append("|");
      if (from != null) 
	token.append(dateFormat.format(from));
      token.append("|");
      if (until != null)
	token.append(dateFormat.format(until));
      token.append("|");
      token.append((set != null ? set: "")).append("|");
      token.append(count);
      return token.toString();
    }
    return null;
  }

  public boolean generateRecord(StringBuffer xml) {
    more = next.before(realEnd);
    if (more) { 
      xml.append(recordStart);
      getRecordHeader(xml);
      if (recordMode)
	xml.append(metaData).append(oai_dcStart).append(generateDate()).append(oai_dcEnd).append(metaDataEnd);
      xml.append(recordEnd);
    }
    return more;
  }

  private String generateDate() {
    return    "					<dc:date>" + dateFormat.format(next) + " </dc:date>\n"; 
  }

  void getRecordHeader(StringBuffer xml) {
    xml.append(
	"			<header>\n" + 
	"				<identifier>" + next.getTime() / 1000 + "</identifier>\n" + 
	"				<datestamp>" + dateFormat.format(next)  + "</datestamp>\n" +
	(set != null ? 
	"				<setSpec>" +  set + "</setSpec>\n" : "")    + 
	"			</header>\n");
  }

  public boolean isRecordMode() {
    return recordMode;
  }

  public void setRecordMode(boolean recordMode) {
    this.recordMode = recordMode;
  }

  protected int getCount() {
    return count;
  }

  protected void setCount(int count) {
    this.count = count;
  }

  protected String getSet() {
    return set;
  }

  protected void setSet(String set) {
    this.set = set;
  }

  protected String getPrefix() {
    return prefix;
  }

  protected void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  protected Date getFrom() {
    return from;
  }

  protected void setFrom(Date from) {
    this.from = from;
  }

  protected Date getUntil() {
    return until;
  }

  protected void setUntil(Date until) {
    this.until = until;
  }
}
