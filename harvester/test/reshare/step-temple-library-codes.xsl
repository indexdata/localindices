<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="@* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>

  <!-- Map legacy code for the library/institution to a FOLIO resource identifier
       type UUID. Used for qualifying a local record identifier with the library
       it originated from in context of a shared index setup where the Instance
       represents bib records from multiple libraries.
  -->
  <xsl:template match="identifiers/arr/i/identifierType">
    <identifierTypeId>17bb9b44-0063-44cc-8f1a-ccbb6188060b</identifierTypeId>
  </xsl:template>

  <!-- Map legacy location code to a FOLIO location UUID -->
  <xsl:template match="holdingsRecords/arr/i/permanentLocation">
    <permanentLocationId>
      <xsl:choose>
        <xsl:when test=".='ASRS'">07d43ef5-b53c-4b5a-bafa-e86e28f8babc</xsl:when>
        <xsl:when test=".='MAIN'">8b0fa6c3-d77e-4321-936c-07f623b386fa</xsl:when>
        <xsl:otherwise>87038e41-0990-49ea-abd9-1ad00a786e45</xsl:otherwise>  <!-- Any Temple location -->
      </xsl:choose>
    </permanentLocationId>
  </xsl:template>

</xsl:stylesheet>
