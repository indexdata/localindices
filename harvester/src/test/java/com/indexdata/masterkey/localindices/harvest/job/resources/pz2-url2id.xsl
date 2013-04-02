<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:pz="http://www.indexdata.com/pazpar2/1.0">
  <xsl:output indent="yes" method="xml"/>

  <xsl:template match="@* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>

  <!-- Copy the url into id --> 
  <xsl:template match="metadata[@type='url']">
    <pz:metadata type="id">
        <xsl:value-of select="."/>
    </pz:metadata>
    <pz:metadata type="url">
        <xsl:value-of select="."/>
    </pz:metadata>
  </xsl:template>

</xsl:stylesheet>
