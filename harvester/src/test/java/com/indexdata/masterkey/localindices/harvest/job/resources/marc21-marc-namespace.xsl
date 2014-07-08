<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:pz="http://www.indexdata.com/pazpar2/1.0"
    xmlns:marc="http://www.loc.gov/MARC21/slim" 
    xmlns="http://www.loc.gov/MARC21/slim" >

  
  <xsl:output indent="yes" method="xml" version="1.0" encoding="UTF-8"/>

<!-- Extract metadata from MARC21/USMARC 
      http://www.loc.gov/marc/bibliographic/ecbdhome.html
-->  

  <xsl:param name="open_url_resolver"/>
  <!--<xsl:variable name="resolver">http://zeus.lib.uoc.gr:3210/sfxtst3</xsl:variable>-->
 
  <xsl:template name="insert-md-openurl">
  
    <xsl:value-of select="$open_url_resolver" /><xsl:text>?generatedby=pz2</xsl:text>
    <xsl:call-template name="ou-parse-author" />
    <xsl:call-template name="ou-parse-date" />
    <xsl:call-template name="ou-parse-volume" />
    <xsl:call-template name="ou-parse-any">
      <xsl:with-param name="field_name" select="string('isbn')" />
    </xsl:call-template>
    <xsl:call-template name="ou-parse-any">
      <xsl:with-param name="field_name" select="string('issn')" />
    </xsl:call-template>
    <xsl:call-template name="ou-parse-any">
      <xsl:with-param name="field_name" select="string('title')" />
    </xsl:call-template>
    <xsl:call-template name="ou-parse-any">
      <xsl:with-param name="field_name" select="string('atitle')" />
    </xsl:call-template>

  </xsl:template>
 
  <!-- parsing raw string data -->
  
  <xsl:template name="ou-parse-author" >
    <xsl:variable name="author">
      <xsl:call-template name="ou-author" />
    </xsl:variable>
    
    <xsl:variable name="aulast" select="normalize-space(substring-before($author, ','))"/>

    <xsl:variable name="aufirst" 
      select="substring-before( normalize-space(substring-after($author, ',')), ' ')"/>

    <xsl:if test="$aulast != ''">
      <xsl:text>&amp;aulast=</xsl:text>
      <xsl:value-of select="$aulast" />
    </xsl:if>

    <xsl:if test="string-length( translate($aufirst, '.', '') ) &gt; 1" >
      <xsl:text>&amp;aufirst=</xsl:text>
      <xsl:value-of select="$aufirst" />
    </xsl:if>

  </xsl:template>

  <xsl:template name="ou-parse-volume">
    <xsl:variable name="volume">
      <xsl:call-template name="ou-volume" />
    </xsl:variable>

    <xsl:variable name="vol" select="substring-after($volume, 'Vol')"/>
    <xsl:variable name="issue" select="false()" />
    <xsl:variable name="spage" select="false()" />

    <xsl:if test="$vol">
      <xsl:text>&amp;volume=</xsl:text>
      <xsl:value-of select="$vol" />
    </xsl:if>

    <xsl:if test="$issue">
      <xsl:text>&amp;issue=</xsl:text>
      <xsl:value-of select="$issue" />
    </xsl:if>
    
    <xsl:if test="$spage">
      <xsl:text>&amp;spage=</xsl:text>
      <xsl:value-of select="$vol" />
    </xsl:if>

  </xsl:template>


  <xsl:template name="ou-parse-date">
    <xsl:variable name="date">
      <xsl:call-template name="ou-date" />
    </xsl:variable>

    <xsl:variable name="parsed_date" select="translate($date, '.[]c;', '')"/>

    <xsl:if test="$parsed_date">
      <xsl:text>&amp;date=</xsl:text>
      <xsl:value-of select="$parsed_date" />
    </xsl:if>

  </xsl:template>

  
  <xsl:template name="ou-parse-any">
    <xsl:param name="field_name" />

    <xsl:variable name="field_value">
      <xsl:choose>

      <xsl:when test="$field_name = 'isbn'">
        <xsl:call-template name="ou-isbn"/>
      </xsl:when>

      <xsl:when test="$field_name = 'issn'">
        <xsl:call-template name="ou-issn"/>
      </xsl:when>
      
      <xsl:when test="$field_name = 'atitle'">
        <xsl:call-template name="ou-atitle"/>
      </xsl:when>
     
      <xsl:when test="$field_name = 'title'">
        <xsl:call-template name="ou-title"/>
      </xsl:when>

      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="digits" select="1234567890"/>

    <xsl:variable name="parsed_value">
      <xsl:choose>

      <xsl:when test="$field_name = 'isbn'">
        <xsl:value-of select="translate($field_value, translate($field_value, concat($digits, 'X'), ''), '')"/>
      </xsl:when>

      <xsl:when test="$field_name = 'issn'">
        <xsl:value-of select="translate($field_value, translate($field_value, concat($digits, '-', 'X'), ''), '')"/>
      </xsl:when>
      
      <xsl:when test="$field_name = 'atitle'">
        <xsl:value-of select="translate(normalize-space($field_value), '.', '')"/>
      </xsl:when>
     
      <xsl:when test="$field_name = 'title'">
        <xsl:value-of select="translate(normalize-space($field_value), '.', '')"/>
      </xsl:when>

      </xsl:choose>
    </xsl:variable>


    <xsl:if test="$parsed_value != ''">
      <xsl:text>&amp;</xsl:text>
      <xsl:value-of select="$field_name" />
      <xsl:text>=</xsl:text>
      <xsl:value-of select="$parsed_value" />
    </xsl:if>

  </xsl:template>


  <xsl:template name="ou-author" >
  <!-- what to do with multiple authors??-->
    <xsl:for-each select="marc:datafield[@tag='100' or @tag='700']">
      <xsl:value-of select="marc:subfield[@code='a']"/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="ou-title" >
  <!-- if 773 exists its a journal/article -->
    <xsl:choose>
    
      <xsl:when test="marc:datafield[@tag='773']/marc:subfield[@code='t']">
        <xsl:value-of select="marc:datafield[@tag='773']/marc:subfield[@code='t']"/>
      </xsl:when>

      <xsl:when test="marc:datafield[@tag='245']/marc:subfield[@code='a']">
        <xsl:value-of select="marc:datafield[@tag='245']/marc:subfield[@code='a']"/>
      </xsl:when>

    </xsl:choose>
  </xsl:template>

  
  <xsl:template name="ou-atitle" >
    <!-- return value only if article or journal -->
    <xsl:if test="marc:datafield[@tag='773']">
      <xsl:value-of select="marc:datafield[@tag='245']/marc:subfield[@code='a']"/>
    </xsl:if>
  </xsl:template>


  <xsl:template name="ou-date" >
    <xsl:for-each select="marc:datafield[@tag='260']">
      <xsl:value-of select="marc:subfield[@code='c']"/>
    </xsl:for-each>
  </xsl:template>

  
  <xsl:template name="ou-isbn" >
  <!-- if 773 exists its a journal/article -->
    <xsl:choose>  
    
      <xsl:when test="marc:datafield[@tag='773']/marc:subfield[@code='z']">
        <xsl:value-of select="marc:datafield[@tag='773']/marc:subfield[@code='z']"/>
      </xsl:when>
      
      <xsl:when test="marc:datafield[@tag='020']/marc:subfield[@code='a']">
        <xsl:value-of select="marc:datafield[@tag='020']/marc:subfield[@code='a']"/>
      </xsl:when>

    </xsl:choose>
  </xsl:template>

  
  <xsl:template name="ou-issn" >
  <!-- if 773 exists its a journal/article -->
    <xsl:choose>
    
      <xsl:when test="marc:datafield[@tag='773']/marc:subfield[@code='x']">
        <xsl:value-of select="marc:datafield[@tag='773']/marc:subfield[@code='x']"/>
      </xsl:when>

      <xsl:when test="marc:datafield[@tag='022']/marc:subfield[@code='a']">
        <xsl:value-of select="marc:datafield[@tag='022']/marc:subfield[@code='a']"/>
      </xsl:when>

      </xsl:choose>
  </xsl:template>

  
  <xsl:template name="ou-volume" >
    <xsl:if test="marc:datafield[@tag='773']">
      <xsl:value-of select="marc:datafield[@tag='773']/marc:subfield[@code='g']"/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="/">
    <pz:collection> 
      <xsl:apply-templates />
    </pz:collection>
  </xsl:template>              
  
  <xsl:template match="//delete">
    <xsl:copy-of select="."/>                                                                                                                                                                               
  </xsl:template>              
  
  <xsl:template match="//marc:record" >
    <xsl:variable name="title_medium" select="marc:datafield[@tag='245']/marc:subfield[@code='h']"/>
    <xsl:variable name="journal_title" select="marc:datafield[@tag='773']/marc:subfield[@code='t']"/>
    <xsl:variable name="electronic_location_url" select="marc:datafield[@tag='856']/marc:subfield[@code='u']"/>
    <xsl:variable name="fulltext_a" select="marc:datafield[@tag='900']/marc:subfield[@code='a']"/>
    <xsl:variable name="fulltext_b" select="marc:datafield[@tag='900']/marc:subfield[@code='b']"/>
    <xsl:variable name="medium">
      <xsl:choose>
    <xsl:when test="$title_medium">
      <xsl:value-of select="substring-after(substring-before($title_medium,']'),'[')"/>
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
      <xsl:for-each select="marc:controlfield[@tag='001']">
        <pz:metadata type="id">
          <xsl:value-of select="."/>
        </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='010']">
        <pz:metadata type="lccn">
      <xsl:value-of select="marc:subfield[@code='a']"/>
    </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='020']">
        <pz:metadata type="isbn">
      <xsl:value-of select="marc:subfield[@code='a']"/>
    </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='022']">
        <pz:metadata type="issn">
      <xsl:value-of select="marc:subfield[@code='a']"/>
    </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='027']">
        <pz:metadata type="tech-rep-nr">
      <xsl:value-of select="marc:subfield[@code='a']"/>
    </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='035']">
        <pz:metadata type="system-control-nr">
      <xsl:value-of select="marc:subfield[@code='a']"/>
    </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='100']">
    <pz:metadata type="author">
      <xsl:value-of select="marc:subfield[@code='a']"/>
    </pz:metadata>
    <pz:metadata type="author-title">
      <xsl:value-of select="marc:subfield[@code='c']"/>
    </pz:metadata>
    <pz:metadata type="author-date">
      <xsl:value-of select="marc:subfield[@code='d']"/>
    </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='110']">
    <pz:metadata type="corporate-name">
        <xsl:value-of select="marc:subfield[@code='a']"/>
    </pz:metadata>
    <pz:metadata type="corporate-location">
        <xsl:value-of select="marc:subfield[@code='c']"/>
    </pz:metadata>
    <pz:metadata type="corporate-date">
        <xsl:value-of select="marc:subfield[@code='d']"/>
    </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='111']">
    <pz:metadata type="meeting-name">
        <xsl:value-of select="marc:subfield[@code='a']"/>
    </pz:metadata>
    <pz:metadata type="meeting-location">
        <xsl:value-of select="marc:subfield[@code='c']"/>
    </pz:metadata>
    <pz:metadata type="meeting-date">
        <xsl:value-of select="marc:subfield[@code='d']"/>
    </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='260']">
    <pz:metadata type="date">
        <xsl:value-of select="marc:subfield[@code='c']"/>
    </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='245']">
        <pz:metadata type="title">
          <xsl:value-of select="marc:subfield[@code='a']"/>
        </pz:metadata>
        <pz:metadata type="title-remainder">
          <xsl:value-of select="marc:subfield[@code='b']"/>
        </pz:metadata>
        <pz:metadata type="title-responsibility">
          <xsl:value-of select="marc:subfield[@code='c']"/>
        </pz:metadata>
        <pz:metadata type="title-dates">
          <xsl:value-of select="marc:subfield[@code='f']"/>
        </pz:metadata>
        <pz:metadata type="title-medium">
          <xsl:value-of select="marc:subfield[@code='h']"/>
        </pz:metadata>
        <pz:metadata type="title-number-section">
          <xsl:value-of select="marc:subfield[@code='n']"/>
        </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='250']">
    <pz:metadata type="edition">
        <xsl:value-of select="marc:subfield[@code='a']"/>
    </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='260']">
        <pz:metadata type="publication-place">
      <xsl:value-of select="marc:subfield[@code='a']"/>
    </pz:metadata>
        <pz:metadata type="publication-name">
      <xsl:value-of select="marc:subfield[@code='b']"/>
    </pz:metadata>
        <pz:metadata type="publication-date">
      <xsl:value-of select="marc:subfield[@code='c']"/>
    </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='300']">
    <pz:metadata type="physical-extent">
      <xsl:value-of select="marc:subfield[@code='a']"/>
    </pz:metadata>
    <pz:metadata type="physical-format">
      <xsl:value-of select="marc:subfield[@code='b']"/>
    </pz:metadata>
    <pz:metadata type="physical-dimensions">
      <xsl:value-of select="marc:subfield[@code='c']"/>
    </pz:metadata>
    <pz:metadata type="physical-accomp">
      <xsl:value-of select="marc:subfield[@code='e']"/>
    </pz:metadata>
    <pz:metadata type="physical-unittype">
      <xsl:value-of select="marc:subfield[@code='f']"/>
    </pz:metadata>
    <pz:metadata type="physical-unitsize">
      <xsl:value-of select="marc:subfield[@code='g']"/>
    </pz:metadata>
    <pz:metadata type="physical-specified">
      <xsl:value-of select="marc:subfield[@code='3']"/>
    </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='440']">
    <pz:metadata type="series-title">
      <xsl:value-of select="marc:subfield[@code='a']"/>
    </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag = '500' or @tag = '505' or
            @tag = '518' or @tag = '520' or @tag = '522']">
    <pz:metadata type="description">
            <xsl:value-of select="*/text()"/>
        </pz:metadata>
      </xsl:for-each>
      
      <xsl:for-each select="marc:datafield[@tag='600' or @tag='610' or @tag='611' or @tag='630' or @tag='648' or @tag='650' or @tag='651' or @tag='653' or @tag='654' or @tag='655' or @tag='656' or @tag='657' or @tag='658' or @tag='662' or @tag='69X']">
        <pz:metadata type="subject">
      <xsl:value-of select="marc:subfield[@code='a']"/>
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

      <xsl:for-each select="marc:datafield[@tag='856']">
    <pz:metadata type="electronic-url">
      <xsl:value-of select="marc:subfield[@code='u']"/>
    </pz:metadata>
    <pz:metadata type="electronic-text">
      <xsl:value-of select="marc:subfield[@code='y']"/>
    </pz:metadata>
    <pz:metadata type="electronic-note">
      <xsl:value-of select="marc:subfield[@code='z']"/>
    </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='773']">
        <pz:metadata type="citation">
          <xsl:for-each select="*">
            <xsl:value-of select="normalize-space(.)"/>
            <xsl:text> </xsl:text>
          </xsl:for-each>
        </pz:metadata>
        <xsl:if test="marc:subfield[@code='t']">
          <pz:metadata type="journal-title">
            <xsl:value-of select="marc:subfield[@code='t']"/>
          </pz:metadata>
        </xsl:if>
        <xsl:if test="marc:subfield[@code='g']">
          <pz:metadata type="journal-subpart">
            <xsl:value-of select="marc:subfield[@code='g']"/>
          </pz:metadata>
        </xsl:if>
      </xsl:for-each>

      <pz:metadata type="medium">
    <xsl:value-of select="$medium"/>
      </pz:metadata>
      
      <xsl:for-each select="marc:datafield[@tag='900']/marc:subfield[@code='a']">
        <pz:metadata type="fulltext">
          <xsl:value-of select="."/>
        </pz:metadata>
      </xsl:for-each>

      <!-- <xsl:if test="$fulltext_a">
    <pz:metadata type="fulltext">
      <xsl:value-of select="$fulltext_a"/>
    </pz:metadata>
      </xsl:if> -->

      <xsl:for-each select="marc:datafield[@tag='900']/marc:subfield[@code='b']">
        <pz:metadata type="fulltext">
          <xsl:value-of select="."/>
        </pz:metadata>
      </xsl:for-each>

      <!-- <xsl:if test="$fulltext_b">
    <pz:metadata type="fulltext">
      <xsl:value-of select="$fulltext_b"/>
    </pz:metadata>
      </xsl:if> -->

      <xsl:for-each select="marc:datafield[@tag='907' or @tag='901']">
        <pz:metadata type="iii-id">
      <xsl:value-of select="marc:subfield[@code='a']"/>
    </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='926']">
        <pz:metadata type="holding">
      <xsl:for-each select="marc:subfield">
        <xsl:if test="position() > 1">
          <xsl:text> </xsl:text>
        </xsl:if>
        <xsl:value-of select="."/>
      </xsl:for-each>
        </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='948']">
        <pz:metadata type="holding">
      <xsl:for-each select="marc:subfield">
        <xsl:if test="position() > 1">
          <xsl:text> </xsl:text>
        </xsl:if>
        <xsl:value-of select="."/>
      </xsl:for-each>
        </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="marc:datafield[@tag='991']">
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

</xsl:stylesheet>