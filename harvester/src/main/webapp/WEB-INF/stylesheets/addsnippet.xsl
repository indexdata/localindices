<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:pz="http://www.indexdata.com/pazpar2/1.0"
  xmlns:z="http://indexdata.com/zebra-2.0"
  version="1.0">
  <xsl:output indent="yes" method="xml" version="1.0" encoding="UTF-8"/>

  <xsl:template match="text()"/>
  
  <xsl:template match="pz:record">
      <xsl:copy>
       <xsl:copy-of select="@*"/>
       <xsl:copy-of select="*"/>
       <z:meta name="snippet"/>
      </xsl:copy>
  </xsl:template>

</xsl:stylesheet>