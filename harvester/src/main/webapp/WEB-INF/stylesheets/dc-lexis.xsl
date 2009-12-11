<?xml version="1.0" encoding="UTF-8"?>
<!--

    This stylesheet expects oai/dc records
-->
<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:pz="http://www.indexdata.com/pazpar2/1.0"
    xmlns:oai="http://www.openarchives.org/OAI/2.0/"
    xmlns:dc="http://purl.org/dc/elements/1.1/">

 <xsl:output indent="yes"
        method="xml"
        version="1.0"
        encoding="UTF-8"/>

  <!--
    Lexis is messed up in the following ways:
    * dc:Title instead of dc:title
    * multiple identifiers of different types
    * multiple dates of different types
    * creators and contributors - not sure which are authors
  -->



  <xsl:template match="*">
    <pz:record>

      <pz:metadata type="id">
        <xsl:value-of select="dc:identifier[@type='document.number']"/>
      </pz:metadata>

      <xsl:for-each select="dc:Title">
        <pz:metadata type="title">
          <xsl:value-of select="."/>
        </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="dc:date[@type='document.date']">
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

      <xsl:for-each select="dc:contributor">
        <pz:metadata type="author">
          <xsl:value-of select="."/>
        </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="dc:description">
        <pz:metadata type="description">
          <xsl:value-of select="."/>
        </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="dc:identifier[@scheme='URL']">
        <pz:metadata type="electronic-url">
          <xsl:value-of select="."/>
        </pz:metadata>
      </xsl:for-each>

      <xsl:for-each select="dc:format">
        <pz:metadata type="medium">
          <xsl:value-of select="."/>
        </pz:metadata>
      </xsl:for-each>

    </pz:record>
  </xsl:template>

  <xsl:template match="text()"/>

</xsl:stylesheet>
