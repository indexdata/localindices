
delete from TRANSFORMATION_TRANSFORMATIONSTEP where TRANSFORMATION_ID = 10;
delete from TRANSFORMATION where ID = 10;
insert into TRANSFORMATION (ID, DTYPE, NAME, ENABLED, DESCRIPTION) values (10, 'BasicTransformation', 'OAI-PMH(DC) to PZ', 1, 'Converting from OAI-PMH(DC) to PZ');

delete from TRANSFORMATIONSTEP where ID = 10;
insert into TRANSFORMATIONSTEP (ID, DTYPE, NAME, ENABLED, POSITION, SCRIPT) values ( 10, 'BasicTransformationStep', 'OAIPMH-DC to PZ', 1, 1, '<?xml version="1.0" encoding="UTF-8"?>
<!--

    This stylesheet expects oai/dc records
-->
<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:pz="http://www.indexdata.com/pazpar2/1.0"
    xmlns:oai="http://www.openarchives.org/OAI/2.0/"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:dcterms="http://purl.org/dc/terms/">

  <xsl:output indent="yes"
              method="xml"
              version="1.0"
              encoding="UTF-8"/>

  <xsl:template match="/oai:OAI-PMH">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="oai:ListRecords">
    <pz:collection>
      <xsl:apply-templates/>
    </pz:collection>
  </xsl:template>

  <xsl:template match="oai:record">
    <pz:record>
      <pz:metadata type="id">
        <xsl:value-of select="oai:header/oai:identifier"/>
      </pz:metadata>
      <xsl:if test="oai:header[@status=\'deleted\']">
	<pz:metadata type="record_status">deleted</pz:metadata>
      </xsl:if>
      <xsl:apply-templates/>
    </pz:record>
  </xsl:template>
    
  <xsl:template match="oai:metadata/*">
    <xsl:for-each select="dc:title">
      <pz:metadata type="title">
        <xsl:value-of select="."/>
      </pz:metadata>
    </xsl:for-each>

    <xsl:for-each select="dc:date">
      <pz:metadata type="date">
	<xsl:value-of select="."/>
      </pz:metadata>
    </xsl:for-each>

    <xsl:for-each select="dc:subject">
      <pz:metadata type="subject">
	<xsl:value-of select="."/>
      </pz:metadata>
    </xsl:for-each>

    <xsl:for-each select="dc:creator">
      <pz:metadata type="author">
        <xsl:value-of select="."/>
      </pz:metadata>
    </xsl:for-each>

    <xsl:for-each select="dc:description">
      <pz:metadata type="description">
	<xsl:value-of select="."/>
      </pz:metadata>
    </xsl:for-each>

    <xsl:for-each select="dc:identifier">
      <pz:metadata type="electronic-url">
	<xsl:value-of select="."/>
      </pz:metadata>
    </xsl:for-each>

    <xsl:for-each select="dc:type">
      <pz:metadata type="medium">
	<xsl:value-of select="."/>
      </pz:metadata>
    </xsl:for-each>
      
    <xsl:for-each select="dcterms:bibliographicCitation">
      <pz:metadata type="citation">
        <xsl:value-of select="."/>
      </pz:metadata>
    </xsl:for-each>

  </xsl:template>

  <xsl:template match="text()"/>

</xsl:stylesheet>');

insert into TRANSFORMATION_TRANSFORMATIONSTEP (TRANSFORMATION_ID, STEPS_ID) values (10, 10);

delete from TRANSFORMATION_TRANSFORMATIONSTEP where TRANSFORMATION_ID = 20;
delete from TRANSFORMATION where ID = 20;
insert into TRANSFORMATION (ID, DTYPE, NAME, ENABLED, DESCRIPTION) values (20, 'BasicTransformation', 'OAI-PMH(MARCXML) to PZ', 1, 'Converting from OAI-PMH(MARCXML) to PZ');

delete from TRANSFORMATIONSTEP where ID = 20;
insert into TRANSFORMATIONSTEP (ID, DTYPE, NAME, ENABLED, POSITION, SCRIPT) values ( 20, 'BasicTransformationStep', 'OAIPMH-MARC to MARC21', 1, 1, '<?xml version="1.0" encoding="UTF-8"?>
<!--

    This stylesheet pulls out marc records from the oai-pmh response
    and overwrites the controlfield 001 with oai-identifier
-->
<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:oai="http://www.openarchives.org/OAI/2.0/"
    xmlns:marc="http://www.loc.gov/MARC21/slim"
    xmlns:pz="http://www.indexdata.com/pazpar2/1.0"
    exclude-result-prefixes="oai">


 <xsl:output indent="yes"
        method="xml"
        version="1.0"
        encoding="UTF-8"/>


<xsl:template match="/">
<collection>
  <xsl:apply-templates/>
</collection>

</xsl:template>

  <xsl:template match="//oai:record">
    <xsl:if test="oai:header[@status=\'deleted\']">
      <delete><xsl:attribute name="id"><xsl:value-of select="oai:header/oai:identifier"/></xsl:attribute></delete>
    </xsl:if>
    <xsl:if test="not(oai:header[@status=\'deleted\'])">
      <xsl:variable name="oai-id">
          <xsl:value-of select="oai:header/oai:identifier"/>
      </xsl:variable>

      <xsl:for-each select="oai:metadata/marc:record">
        <xsl:copy>
          <xsl:copy-of select="@*"/>
          <xsl:copy-of select="*"/>
          <pz:metadata type="id">
           <xsl:value-of select="$oai-id"/>
          </pz:metadata>
        </xsl:copy>
      </xsl:for-each>
    </xsl:if>
  </xsl:template>

  <xsl:template match="text()"/>

</xsl:stylesheet>');

delete from TRANSFORMATIONSTEP where ID = 22;
insert into TRANSFORMATIONSTEP (ID, DTYPE, NAME, ENABLED, POSITION, SCRIPT) values ( 22, 'BasicTransformationStep', 'MARC21 to PZ', 1, 2, '<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:pz="http://www.indexdata.com/pazpar2/1.0"
    xmlns:marc="http://www.loc.gov/MARC21/slim">

  
  <xsl:output indent="yes" method="xml" version="1.0" encoding="UTF-8"/>

<!-- Extract metadata from MARC21/USMARC 
      http://www.loc.gov/marc/bibliographic/ecbdhome.html
-->  
  <xsl:include href="pz2-ourl-marc21.xsl" />

  <xsl:template match="/">
    <pz:collection> 
      <xsl:apply-templates />
    </pz:collection>
  </xsl:template>        
  
  <xsl:template match="//delete">
	<xsl:copy-of select="."/>                                                                                                                                                                               
  </xsl:template> 
  
  <xsl:template match="//marc:record">
    <xsl:variable name="title_medium" select="marc:datafield[@tag=\'245\']/marc:subfield[@code=\'h\']"/>
    <xsl:variable name="journal_title" select="marc:datafield[@tag=\'773\']/marc:subfield[@code=\'t\']"/>
    <xsl:variable name="electronic_location_url" select="marc:datafield[@tag=\'856\']/marc:subfield[@code=\'u\']"/>
    <xsl:variable name="fulltext_a" select="marc:datafield[@tag=\'900\']/marc:subfield[@code=\'a\']"/>
    <xsl:variable name="fulltext_b" select="marc:datafield[@tag=\'900\']/marc:subfield[@code=\'b\']"/>
    <xsl:variable name="medium">
      <xsl:choose>
	<xsl:when test="$title_medium">
	  <xsl:value-of select="substring-after(substring-before($title_medium,\']\'),\'[\')"/>
	</xsl:when>
	<xsl:when test="$fulltext_a">
	  <xsl:text>electronic resource</xsl:text>
	</xsl:when>
	<xsl:when test="$fulltext_b">
	  <xsl:text>electronic resource</xsl:text>
	</xsl:when>
	<xsl:when test="$journal_title">
	  <xsl:text>article</xsl:text>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:text>book</xsl:text>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <pz:record>      
      <xsl:for-each select="marc:controlfield[@tag=\'001\']">
        <pz:metadata type="id">
          <xsl:value-of select="."/>
        </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'010\']">
        <pz:metadata type="lccn">
	  <xsl:value-of select="marc:subfield[@code=\'a\']"/>
	</pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'020\']">
        <pz:metadata type="isbn">
	  <xsl:value-of select="marc:subfield[@code=\'a\']"/>
	</pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'022\']">
        <pz:metadata type="issn">
	  <xsl:value-of select="marc:subfield[@code=\'a\']"/>
	</pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'027\']">
        <pz:metadata type="tech-rep-nr">
	  <xsl:value-of select="marc:subfield[@code=\'a\']"/>
	</pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'035\']">
        <pz:metadata type="system-control-nr">
	  <xsl:value-of select="marc:subfield[@code=\'a\']"/>
	</pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'100\']">
	<pz:metadata type="author">
	  <xsl:value-of select="marc:subfield[@code=\'a\']"/>
	</pz:metadata>
	<pz:metadata type="author-title">
	  <xsl:value-of select="marc:subfield[@code=\'c\']"/>
	</pz:metadata>
	<pz:metadata type="author-date">
	  <xsl:value-of select="marc:subfield[@code=\'d\']"/>
	</pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'110\']">
	<pz:metadata type="corporate-name">
	    <xsl:value-of select="marc:subfield[@code=\'a\']"/>
	</pz:metadata>
	<pz:metadata type="corporate-location">
	    <xsl:value-of select="marc:subfield[@code=\'c\']"/>
	</pz:metadata>
	<pz:metadata type="corporate-date">
	    <xsl:value-of select="marc:subfield[@code=\'d\']"/>
	</pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'111\']">
	<pz:metadata type="meeting-name">
	    <xsl:value-of select="marc:subfield[@code=\'a\']"/>
	</pz:metadata>
	<pz:metadata type="meeting-location">
	    <xsl:value-of select="marc:subfield[@code=\'c\']"/>
	</pz:metadata>
	<pz:metadata type="meeting-date">
	    <xsl:value-of select="marc:subfield[@code=\'d\']"/>
	</pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'260\']">
	<pz:metadata type="date">
	    <xsl:value-of select="marc:subfield[@code=\'c\']"/>
	</pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'245\']">
        <pz:metadata type="title">
          <xsl:value-of select="marc:subfield[@code=\'a\']"/>
        </pz:metadata>
        <pz:metadata type="title-remainder">
          <xsl:value-of select="marc:subfield[@code=\'b\']"/>
        </pz:metadata>
        <pz:metadata type="title-responsibility">
          <xsl:value-of select="marc:subfield[@code=\'c\']"/>
        </pz:metadata>
        <pz:metadata type="title-dates">
          <xsl:value-of select="marc:subfield[@code=\'f\']"/>
        </pz:metadata>
        <pz:metadata type="title-medium">
          <xsl:value-of select="marc:subfield[@code=\'h\']"/>
        </pz:metadata>
        <pz:metadata type="title-number-section">
          <xsl:value-of select="marc:subfield[@code=\'n\']"/>
        </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'250\']">
	<pz:metadata type="edition">
	    <xsl:value-of select="marc:subfield[@code=\'a\']"/>
	</pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'260\']">
        <pz:metadata type="publication-place">
	  <xsl:value-of select="marc:subfield[@code=\'a\']"/>
	</pz:metadata>
        <pz:metadata type="publication-name">
	  <xsl:value-of select="marc:subfield[@code=\'b\']"/>
	</pz:metadata>
        <pz:metadata type="publication-date">
	  <xsl:value-of select="marc:subfield[@code=\'c\']"/>
	</pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'300\']">
	<pz:metadata type="physical-extent">
	  <xsl:value-of select="marc:subfield[@code=\'a\']"/>
	</pz:metadata>
	<pz:metadata type="physical-format">
	  <xsl:value-of select="marc:subfield[@code=\'b\']"/>
	</pz:metadata>
	<pz:metadata type="physical-dimensions">
	  <xsl:value-of select="marc:subfield[@code=\'c\']"/>
	</pz:metadata>
	<pz:metadata type="physical-accomp">
	  <xsl:value-of select="marc:subfield[@code=\'e\']"/>
	</pz:metadata>
	<pz:metadata type="physical-unittype">
	  <xsl:value-of select="marc:subfield[@code=\'f\']"/>
	</pz:metadata>
	<pz:metadata type="physical-unitsize">
	  <xsl:value-of select="marc:subfield[@code=\'g\']"/>
	</pz:metadata>
	<pz:metadata type="physical-specified">
	  <xsl:value-of select="marc:subfield[@code=\'3\']"/>
	</pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'440\']">
	<pz:metadata type="series-title">
	  <xsl:value-of select="marc:subfield[@code=\'a\']"/>
	</pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag = \'500\' or @tag = \'505\' or @tag = \'518\' or @tag = \'520\' or @tag = \'522\']">
	<pz:metadata type="description">
            <xsl:value-of select="*/text()"/>
        </pz:metadata>
      </xsl:for-each>
      
      <xsl:for-each select="marc:datafield[@tag=\'600\' or @tag=\'610\' or @tag=\'611\' or @tag=\'630\' or @tag=\'648\' or @tag=\'650\' or @tag=\'651\' or @tag=\'653\' or @tag=\'654\' or @tag=\'655\' or @tag=\'656\' or @tag=\'657\' or @tag=\'658\' or @tag=\'662\' or @tag=\'69X\']">
        <pz:metadata type="subject">
	  <xsl:value-of select="marc:subfield[@code=\'a\']"/>
	</pz:metadata>
	<pz:metadata type="subject-long">
	  <xsl:for-each select="marc:subfield">
	    <xsl:if test="position() > 1">
	      <xsl:text>, </xsl:text>
	    </xsl:if>
	    <xsl:value-of select="."/>
	  </xsl:for-each>
	</pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'856\']">
	<pz:metadata type="electronic-url">
	  <xsl:value-of select="marc:subfield[@code=\'u\']"/>
	</pz:metadata>
	<pz:metadata type="electronic-text">
	  <xsl:value-of select="marc:subfield[@code=\'y\']"/>
	</pz:metadata>
	<pz:metadata type="electronic-note">
	  <xsl:value-of select="marc:subfield[@code=\'z\']"/>
	</pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'773\']">
        <pz:metadata type="citation">
          <xsl:for-each select="*">
            <xsl:value-of select="normalize-space(.)"/>
            <xsl:text> </xsl:text>
          </xsl:for-each>
        </pz:metadata>
        <xsl:if test="marc:subfield[@code=\'t\']">
          <pz:metadata type="journal-title">
            <xsl:value-of select="marc:subfield[@code=\'t\']"/>
          </pz:metadata>
        </xsl:if>
        <xsl:if test="marc:subfield[@code=\'g\']">
          <pz:metadata type="journal-subpart">
            <xsl:value-of select="marc:subfield[@code=\'g\']"/>
          </pz:metadata>
        </xsl:if>
      </xsl:for-each>

      <pz:metadata type="medium">
	<xsl:value-of select="$medium"/>
      </pz:metadata>
      
      <xsl:for-each select="marc:datafield[@tag=\'900\']/marc:subfield[@code=\'a\']">
        <pz:metadata type="fulltext">
          <xsl:value-of select="."/>
        </pz:metadata>
      </xsl:for-each>

      <!-- <xsl:if test="$fulltext_a">
	<pz:metadata type="fulltext">
	  <xsl:value-of select="$fulltext_a"/>
	</pz:metadata>
      </xsl:if> -->

      <xsl:for-each select="marc:datafield[@tag=\'900\']/marc:subfield[@code=\'b\']">
        <pz:metadata type="fulltext">
          <xsl:value-of select="."/>
        </pz:metadata>
      </xsl:for-each>

      <!-- <xsl:if test="$fulltext_b">
	<pz:metadata type="fulltext">
	  <xsl:value-of select="$fulltext_b"/>
	</pz:metadata>
      </xsl:if> -->

      <xsl:for-each select="marc:datafield[@tag=\'907\' or @tag=\'901\']">
        <pz:metadata type="iii-id">
	  <xsl:value-of select="marc:subfield[@code=\'a\']"/>
	</pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'926\']">
        <pz:metadata type="holding">
	  <xsl:for-each select="marc:subfield">
	    <xsl:if test="position() > 1">
	      <xsl:text> </xsl:text>
	    </xsl:if>
	    <xsl:value-of select="."/>
	  </xsl:for-each>
        </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'948\']">
        <pz:metadata type="holding">
	  <xsl:for-each select="marc:subfield">
	    <xsl:if test="position() > 1">
	      <xsl:text> </xsl:text>
	    </xsl:if>
	    <xsl:value-of select="."/>
	  </xsl:for-each>
        </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag=\'991\']">
        <pz:metadata type="holding">
	  <xsl:for-each select="marc:subfield">
	    <xsl:if test="position() > 1">
	      <xsl:text> </xsl:text>
	    </xsl:if>
	    <xsl:value-of select="."/>
	  </xsl:for-each>
        </pz:metadata>
      </xsl:for-each>

      <xsl:if test="$open_url_resolver">
        <pz:metadata type="open-url">
            <xsl:call-template name="insert-md-openurl" />
        </pz:metadata>
      </xsl:if>

      <!--passthrough id data-->
      <xsl:for-each select="pz:metadata">
          <xsl:copy-of select="."/>
      </xsl:for-each>

    </pz:record>    
  </xsl:template>

  
  <xsl:template match="text()"/>

</xsl:stylesheet>');

insert into TRANSFORMATION_TRANSFORMATIONSTEP (TRANSFORMATION_ID, STEPS_ID) values (20, 20);
insert into TRANSFORMATION_TRANSFORMATIONSTEP (TRANSFORMATION_ID, STEPS_ID) values (20, 22);
