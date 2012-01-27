<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:pz="http://www.indexdata.com/pazpar2/1.0">
  <xsl:output indent="yes" method="xml"/>

  <xsl:template match="@* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="pz:metadata[@type='subject']">
    <xsl:call-template name="output-tokens">
	      <xsl:with-param name="list">
		<xsl:value-of select="."/>
	      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <!-- remove empty subject-long -->
  <xsl:template match="pz:metadata[@type='subject-long']">
    <xsl:if test=". and . != '' ">
      <pz:metadata type="subject-long">
	<xsl:value-of select="."/>
      </pz:metadata>
    </xsl:if>
  </xsl:template>

  <xsl:template name="output-tokens">
    <xsl:param name="list"/>
    <xsl:variable name="newlist" select="concat(normalize-space($list), ' -- ')"/>
    <xsl:variable name="first" select="substring-before($newlist, ' -- ')"/>
    <xsl:variable name="remaining" select="substring-after($newlist, ' -- ')"/>
    <xsl:variable name="second" select="substring-before($remaining, ' -- ')"/>

    <xsl:if test="$first and $first != '--'">
      <pz:metadata type='subject'>
	<xsl:value-of select="$first"/>
	<xsl:if test="$second and $second != '--' ">
	  <xsl:text> -- </xsl:text>
	  <xsl:value-of select="$second"/>
	</xsl:if>
      </pz:metadata>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>
