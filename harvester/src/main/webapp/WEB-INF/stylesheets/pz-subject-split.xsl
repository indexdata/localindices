<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
		xmlns:pz="http://www.indexdata.com/pazpar2/1.0"
>
  <xsl:output indent="yes" method="xml"/>


  <xsl:template match="@* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="/collection/pz:record/pz:metadata[@type='subject']">
    <xsl:for-each select="tokenize(.,'; *|-- *| - |\. +|, *|\)|\(|\.$')">
      <xsl:if test="normalize-space(.) != ''">
	<pz:metadata type="subject">
	  <xsl:value-of select="normalize-space(.)"/>
	</pz:metadata>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
