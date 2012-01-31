<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="2.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns:dcterms="http://purl.org/dc/terms/"
	xmlns:dcmitype="http://purl.org/dc/dcmitype/"
	xmlns:cc="http://web.resource.org/cc/"
	xmlns:pgterms="http://www.gutenberg.org/rdfterms/"
	xml:base="http://www.gutenberg.org/feeds/catalog.rdf"
>
  <xsl:template match="@* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>


  <!-- remove empty subject-long -->
  <xsl:template match="dc:creator">
    <xsl:variable name="author" select="replace(string(.), ', [0-9]{4}\-[0-9]{4}$', '')" />
    <xsl:if test="$author and $author != '' ">
      <dc:creator rdf:parseType="Literal">
	<xsl:value-of select="$author"/>
      </dc:creator>
    </xsl:if>
  </xsl:template>


</xsl:stylesheet>
