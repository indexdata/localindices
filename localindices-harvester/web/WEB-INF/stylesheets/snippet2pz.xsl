<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:pz="http://www.indexdata.com/pazpar2/1.0"
  xmlns:z="http://indexdata.com/zebra-2.0"
  exclude-result-prefixes="z"
  version="1.0">
  <xsl:output indent="yes" method="xml" version="1.0" encoding="UTF-8"/>
  
  <xsl:template match="pz:record">
      <xsl:copy>
       <xsl:copy-of select="@*"/>
       <xsl:apply-templates/>
      </xsl:copy>
  </xsl:template>

  <xsl:template match="z:record/z:snippet">
        <pz:metadata type="snippet">
          <xsl:apply-templates mode="snippet"/>
        </pz:metadata>
  </xsl:template>

  <xsl:template match="z:s" mode="snippet">
    <xsl:text>@sb</xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>@se</xsl:text>
  </xsl:template>
  
  <xsl:template match="*">
      <xsl:copy-of select="."/>
  </xsl:template>
  
 </xsl:stylesheet>
