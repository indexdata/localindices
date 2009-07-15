<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:pz="http://www.indexdata.com/pazpar2/1.0"
  version="1.0">
  <xsl:output indent="yes" method="xml" version="1.0" encoding="UTF-8"/>

  <xsl:template match="text()"/>
  
  <xsl:template match="pz:record">
      <xsl:copy>
       <xsl:copy-of select="@*"/>
       <xsl:attribute name="mergekey">
           <xsl:text>url </xsl:text>
           <xsl:value-of select="pz:metadata[@type='electronic-url']"/>
       </xsl:attribute>
       <xsl:copy-of select="*"/>
      </xsl:copy>
  </xsl:template>

</xsl:stylesheet>