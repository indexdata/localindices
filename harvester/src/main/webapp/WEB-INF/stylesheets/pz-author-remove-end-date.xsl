<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
   xmlns:pz="http://www.indexdata.com/pazpar2/1.0"
>
  <xsl:template match="@* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>


  <!-- remove empty subject-long -->
  <xsl:template match="pz:metadata[@type='author']">
    <xsl:variable name="author" select="replace(string(.), ', [0-9]{4}\-[0-9]{4}$', '')" />
    <xsl:if test="$author and $author != '' ">
      <pz:metadata type="author">
	<xsl:value-of select="$author"/>
      </pz:metadata>
    </xsl:if>
  </xsl:template>


</xsl:stylesheet>
