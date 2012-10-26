<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
   xmlns:pz="http://www.indexdata.com/pazpar2/1.0">

  <xsl:template match="@* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="pz:metadata[@type='electronic-url']">
       <xsl:if test="contains(.,'Thumbnail: http://')">
           <pz:metadata type="thumburl">
              <xsl:value-of select="substring-after(.,'Thumbnail: ')"/>
           </pz:metadata>
       </xsl:if>
       <xsl:if test="contains(.,'Image View: http://')">
           <pz:metadata type="electronic-url">
              <xsl:value-of select="substring-after(.,'Image View: ')"/>
           </pz:metadata>
       </xsl:if>  
  </xsl:template>

</xsl:stylesheet>
