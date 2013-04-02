<xsl:stylesheet version="2.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:pz="http://www.indexdata.com/pazpar2/1.0" >
  <xsl:output indent="yes" method="xml"/>

  <xsl:template match="@* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="pz:record"> 
    <pz:record>
      <xsl:if test="not(pz:metadata[@type='id'])">
        <pz:metadata type="id">
          <xsl:value-of select="pz:metadata[@type='author']" />
          <xsl:text> - </xsl:text>
          <xsl:value-of select="pz:metadata[@type='title']" />
          <xsl:text> - </xsl:text>
          <xsl:value-of select="pz:metadata[@type='date']" />
        </pz:metadata>
        <xsl:apply-templates />
      </xsl:if>
      <xsl:if test="pz:metadata[@type='id']">
        <xsl:apply-templates />
      </xsl:if>
    </pz:record>
  </xsl:template>

</xsl:stylesheet>
