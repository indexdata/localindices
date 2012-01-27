<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output indent="yes" method="xml"/>


  <xsl:template match="@* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="/add/doc/field[@name='subject']">
    <xsl:for-each select="tokenize(.,'; *|-- *| - |\. +|, *|\)|\(|\.$')">
      <xsl:if test="normalize-space(.) != ''">
	<field name="subject">
	  <xsl:value-of select="normalize-space(.)"/>
	</field>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
