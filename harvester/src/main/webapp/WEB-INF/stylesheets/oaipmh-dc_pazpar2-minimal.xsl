<?xml version="1.0" encoding="UTF-8"?>
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
      <xsl:apply-templates/>
    </pz:record>
  </xsl:template>

  <xsl:template match="text()"/>

</xsl:stylesheet>
