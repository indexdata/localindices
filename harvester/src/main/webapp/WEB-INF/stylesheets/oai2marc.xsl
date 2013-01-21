<?xml version="1.0" encoding="UTF-8"?>
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
    <xsl:if test="oai:header[@status='deleted']">
      <delete><xsl:attribute name="id"><xsl:value-of select="oai:header/oai:identifier"/></xsl:attribute></delete>
    </xsl:if>
    <xsl:if test="not(oai:header[@status='deleted'])">
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

</xsl:stylesheet>
