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
    exclude-result-prefixes="oai">


 <xsl:output indent="yes"
        method="xml"
        version="1.0"
        encoding="UTF-8"/>

  <xsl:template match="marc:record">
  <xsl:copy>
   <xsl:copy-of select="@*"/>
   <xsl:apply-templates/>
  </xsl:copy>
  </xsl:template>

  <xsl:template match="marc:leader">
    <xsl:copy-of select="."/>
    <marc:controlfield tag="001"><xsl:value-of select="/oai:record/oai:header/oai:identifier"/></marc:controlfield>
  </xsl:template>

  <xsl:template match="marc:controlfield">
      <xsl:if test="@tag != '001'">
          <xsl:copy-of select="."/>
      </xsl:if>
  </xsl:template>

  <xsl:template match="marc:datafield">
    <xsl:copy-of select="."/>
  </xsl:template>

  <xsl:template match="text()"/>

</xsl:stylesheet>